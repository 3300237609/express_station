package com.example.station.service;

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
import java.util.List;

/**
 * 通用 Redis 缓存服务（业务无关的基础设施组件）。
 * <p>
 * 职责：JSON 读写（统一降级）、事务 afterCommit 前缀失效、各业务 Key 构造。
 * 业务 Service 只依赖本类做缓存编排，不直接接触 StringRedisTemplate。
 * </p>
 * 一致性策略与快递缓存一致：Cache-Aside + 写后失效（Delete）+ TTL 兜底；
 * Redis 故障时所有操作静默降级，业务自动回源数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommonCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    // ==================== Key 前缀 ====================

    public static final String COMPANY_PREFIX = "company:";
    public static final String CONFIG_PREFIX = "config:";
    public static final String SHELF_PREFIX = "shelf:";
    public static final String LOCATION_PREFIX = "location:";
    public static final String USER_PREFIX = "user:";
    public static final String COURIER_PREFIX = "courier:";
    public static final String NOTIFY_PREFIX = "notify:";
    public static final String STAT_PREFIX = "stat:";

    // ==================== TTL 策略 ====================

    /** 基础字典数据（启用公司、配置）：30 分钟 */
    public static final Duration TTL_LONG = Duration.ofMinutes(30);
    /** 详情数据：10 分钟 */
    public static final Duration TTL_DETAIL = Duration.ofMinutes(10);
    /** 分页列表：5 分钟 */
    public static final Duration TTL_LIST = Duration.ofMinutes(5);
    /** 高频变化数据（货架网格、通知）：30 秒 */
    public static final Duration TTL_SHORT = Duration.ofSeconds(30);
    /** 统计聚合：60 秒 */
    public static final Duration TTL_STAT = Duration.ofSeconds(60);

    /**
     * 分页缓存载体（list 数据 + total 总数 + 分页字段，命中后原样还原 R）
     *
     * @param list     当前页数据
     * @param total    总条数
     * @param pageNum  实际页码
     * @param pageSize 实际每页条数
     * @param pages    总页数
     */
    public record CachedPage<T>(List<T> list, long total, int pageNum, int pageSize, int pages) {
    }

    // ==================== 通用读（统一降级） ====================

    /**
     * 按 Class 读取缓存对象
     */
    public <T> T get(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.debug("【通用缓存】读取降级 key={}：{}", key, e.getMessage());
            return null;
        }
    }

    /**
     * 按 TypeReference 读取泛型对象（List / 泛型载体用）
     */
    public <T> T get(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.debug("【通用缓存】读取降级 key={}：{}", key, e.getMessage());
            return null;
        }
    }

    // ==================== 通用写（统一降级） ====================

    /**
     * 写入缓存（null 值不缓存，避免空键堆积）
     */
    public void put(String key, Object value, Duration ttl) {
        if (value == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception e) {
            log.debug("【通用缓存】写入降级 key={}：{}", key, e.getMessage());
        }
    }

    // ==================== 写后失效 ====================

    /**
     * 前缀失效（事务内注册 afterCommit，提交后才清；无事务立即执行）。
     * 多个前缀可一次传入，如入库后同时清货架与货位。
     *
     * @param prefixes Key 前缀（含尾部冒号或按业务约定）
     */
    public void evictPrefixAfterCommit(String... prefixes) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteByPrefixes(prefixes);
                }
            });
        } else {
            deleteByPrefixes(prefixes);
        }
    }

    /**
     * 立即按前缀批量删除（SCAN 游标，非 KEYS 阻塞）
     */
    public void deleteByPrefixes(String... prefixes) {
        try {
            for (String prefix : prefixes) {
                deleteByPrefix(prefix);
            }
        } catch (Exception e) {
            log.debug("【通用缓存】前缀失效降级：{}", e.getMessage());
        }
    }

    private void deleteByPrefix(String prefix) {
        ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(200).build();
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            java.util.List<byte[]> keys = new java.util.ArrayList<>();
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

    // ==================== 各业务 Key 构造 ====================

    // ---------- 快递公司 ----------

    public String companyEnabledKey() {
        return COMPANY_PREFIX + "enabled";
    }

    public String companyDetailKey(Long id) {
        return COMPANY_PREFIX + "detail:" + id;
    }

    public String companyListKey(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        return COMPANY_PREFIX + "list:" + nz(keyword) + "|" + nz(status) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    public String companyVolumeKey() {
        return COMPANY_PREFIX + "volume";
    }

    // ---------- 系统配置 ----------

    public String configListKey() {
        return CONFIG_PREFIX + "list";
    }

    public String configValueKey(String configKey) {
        return CONFIG_PREFIX + "value:" + nz(configKey);
    }

    // ---------- 货架 ----------

    public String shelfDetailKey(Long id) {
        return SHELF_PREFIX + "detail:" + id;
    }

    public String shelfListKey(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        return SHELF_PREFIX + "list:" + nz(keyword) + "|" + nz(status) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    // ---------- 货位 ----------

    public String locationDetailKey(Long id) {
        return LOCATION_PREFIX + "detail:" + id;
    }

    public String locationListKey(Long shelfId, Integer status, Integer pageNum, Integer pageSize) {
        return LOCATION_PREFIX + "list:" + nz(shelfId) + "|" + nz(status) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    /** 货架可视化全量网格（shelfId 空 = 全部货架） */
    public String locationGridKey(Long shelfId) {
        return LOCATION_PREFIX + "grid:" + nz(shelfId);
    }

    // ---------- 用户 ----------

    public String userDetailKey(Long id) {
        return USER_PREFIX + "detail:" + id;
    }

    public String userListKey(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        return USER_PREFIX + "list:" + nz(keyword) + "|" + nz(status) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    public String userPhoneKey(String phone) {
        return USER_PREFIX + "phone:" + nz(phone);
    }

    // ---------- 快递员 ----------

    public String courierListKey(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        return COURIER_PREFIX + "list:" + nz(keyword) + "|" + nz(status) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    // ---------- 消息通知 ----------

    public String notifyListKey(Long userId, Integer isRead, String type, Integer pageNum, Integer pageSize) {
        return NOTIFY_PREFIX + userId + ":list:" + nz(isRead) + "|" + nz(type) + "|" + nz(pageNum) + "|" + nz(pageSize);
    }

    public String notifyUnreadKey(Long userId) {
        return NOTIFY_PREFIX + userId + ":unread";
    }

    /** 指定用户的通知 Key 前缀（标记已读/单发后清该用户全部通知缓存） */
    public String notifyUserPrefix(Long userId) {
        return NOTIFY_PREFIX + userId + ":";
    }

    // ==================== 工具 ====================

    /** 空值占位（null 归一为下划线，保证 key 稳定可复现） */
    private String nz(Object value) {
        return value == null ? "_" : String.valueOf(value);
    }
}
