package com.example.station.service;

import com.example.station.vo.ExpressVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 快递信息 Redis 缓存服务（Cache-Aside 模式）
 * <p>
 * 缓存对象与 Key 设计：
 * <ul>
 *   <li>{@code express:detail:{id}} —— 快递详情（按主键），TTL 10 分钟；</li>
 *   <li>{@code express:no:{expressNo}} —— 按单号查询结果，TTL 10 分钟；</li>
 *   <li>{@code express:list:{条件}} —— 管理分页列表（含 total），TTL 60 秒；</li>
 *   <li>{@code express:my:{userId}:{条件}} —— 我的快递分页，TTL 60 秒。</li>
 * </ul>
 * 写操作统一走 {@link #evictExpress}：精确删除详情/单号两个 Key，
 * 并 SCAN 清理全部列表 Key（列表条件组合多，无法精确失效，故前缀删除 + 短 TTL 双保险）。
 * </p>
 * <p>
 * 可用性优先：Redis 宕机/超时/序列化异常时所有方法静默降级（debug 日志），
 * 业务链路自动回退为直查数据库，不影响快递功能正确性。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpressCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public static final String DETAIL_PREFIX = "express:detail:";
    public static final String NO_PREFIX = "express:no:";
    public static final String LIST_PREFIX = "express:list:";
    public static final String MY_PREFIX = "express:my:";

    /** 详情/单号缓存有效期 */
    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    /** 列表缓存有效期（写后前缀清理，TTL 仅作兜底） */
    private static final Duration LIST_TTL = Duration.ofSeconds(60);

    private static final TypeReference<CachedPage> PAGE_TYPE = new TypeReference<>() {};

    /** 分页缓存载体：列表数据 + 总条数（Jackson 2.12+ 支持 record 序列化） */
    public record CachedPage(List<ExpressVO> list, long total) {}

    // ==================== 详情 ====================

    public ExpressVO getDetail(Long id) {
        if (id == null) {
            return null;
        }
        return read(DETAIL_PREFIX + id, ExpressVO.class);
    }

    public void putDetail(ExpressVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        write(DETAIL_PREFIX + vo.getId(), vo, DETAIL_TTL);
    }

    // ==================== 单号查询 ====================

    public ExpressVO getByExpressNo(String expressNo) {
        if (expressNo == null || expressNo.isEmpty()) {
            return null;
        }
        return read(NO_PREFIX + expressNo, ExpressVO.class);
    }

    public void putByExpressNo(ExpressVO vo) {
        if (vo == null || vo.getExpressNo() == null) {
            return;
        }
        write(NO_PREFIX + vo.getExpressNo(), vo, DETAIL_TTL);
    }

    // ==================== 分页列表 ====================

    public CachedPage getPage(String key) {
        String json = safeGet(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PAGE_TYPE);
        } catch (Exception e) {
            log.debug("【快递缓存】分页反序列化失败 key={}", key);
            return null;
        }
    }

    public void putPage(String key, List<ExpressVO> list, long total) {
        write(key, new CachedPage(list, total), LIST_TTL);
    }

    /** 管理列表缓存 Key（参数顺序固定，null 统一占位，保证同条件同 Key） */
    public String listKey(String keyword, String pickupCode, String receiverName, String receiverPhone,
                          Long companyId, String status, String arrivalStart, String arrivalEnd,
                          Integer pageNum, Integer pageSize) {
        return LIST_PREFIX + String.join("|",
                str(keyword), str(pickupCode), str(receiverName), str(receiverPhone),
                str(companyId), str(status), str(arrivalStart), str(arrivalEnd),
                str(pageNum), str(pageSize));
    }

    /** 我的快递缓存 Key */
    public String myListKey(Long userId, String keyword, String status,
                            Integer pageNum, Integer pageSize) {
        return MY_PREFIX + str(userId) + "|" + String.join("|",
                str(keyword), str(status), str(pageNum), str(pageSize));
    }

    // ==================== 写后失效 ====================

    /**
     * 快递写操作后失效缓存。
     * <p>
     * 在事务内调用时注册 afterCommit 回调——事务提交后才清缓存，
     * 避免事务回滚但缓存已被误删；无事务上下文时立即执行。
     * </p>
     *
     * @param id        快递ID（可空）
     * @param expressNo 快递单号（可空）
     */
    public void evictExpress(Long id, String expressNo) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doEvict(id, expressNo);
                }
            });
        } else {
            doEvict(id, expressNo);
        }
    }

    private void doEvict(Long id, String expressNo) {
        try {
            List<String> exactKeys = new ArrayList<>();
            if (id != null) {
                exactKeys.add(DETAIL_PREFIX + id);
            }
            if (expressNo != null && !expressNo.isEmpty()) {
                exactKeys.add(NO_PREFIX + expressNo);
            }
            if (!exactKeys.isEmpty()) {
                redisTemplate.delete(exactKeys);
            }
            // 列表条件组合多，前缀扫描一次性清干净，保证写后列表立即新鲜
            deleteByPrefix(LIST_PREFIX);
            deleteByPrefix(MY_PREFIX);
        } catch (Exception e) {
            log.debug("【快递缓存】失效操作降级：{}", e.getMessage());
        }
    }

    /**
     * SCAN 前缀批量删除（游标非阻塞，比 KEYS 安全；本站 Key 规模小）。
     */
    private void deleteByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(200).build();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            List<byte[]> keys = new ArrayList<>();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                connection.keyCommands().del(keys.toArray(new byte[0][]));
            }
            return null;
        });
    }

    // ==================== 基础读写（统一降级） ====================

    private String safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.debug("【快递缓存】读取降级 key={}：{}", key, e.getMessage());
            return null;
        }
    }

    private <T> T read(String key, Class<T> type) {
        String json = safeGet(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.debug("【快递缓存】反序列化失败 key={}", key);
            return null;
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.debug("【快递缓存】写入降级 key={}：{}", key, e.getMessage());
        }
    }

    private static String str(Object value) {
        return value == null ? "_" : value.toString();
    }
}
