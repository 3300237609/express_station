package com.example.station.service.Impl;

import com.example.station.common.R;
import com.example.station.annotation.constant.ConfigConstants;
import com.example.station.annotation.constant.NotificationConstants;
import com.example.station.annotation.constant.RoleConstants;
import com.example.station.dto.NotificationPublishDTO;
import com.example.station.entity.Notification;
import com.example.station.mapper.NotificationMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.service.NotificationService;
import com.example.station.service.CommonCacheService;
import com.example.station.service.SysConfigService;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.NotificationVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息通知业务实现（事件发送受 notification.enabled 配置开关控制）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    /** 批量插入分片大小（防止一次写入行数过多） */
    private static final int BATCH_SIZE = 500;

    private final NotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;
    private final SysConfigService sysConfigService;
    private final CommonCacheService cache;

    @Override
    public R list(Integer isRead, String type, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与筛选条件校验
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        if (isRead != null && isRead != 0 && isRead != 1) {
            return R.error("已读状态不正确，只能为0或1！");
        }
        Long userId = currentUserId();
        if (userId == null) {
            return R.error("未获取到当前用户信息！");
        }
        String typeTrim = StringUtils.hasText(type) ? type.trim() : null;
        // 1.5 先查缓存（key 按用户 + 筛选 + 分页，30 秒 TTL，已读/新消息后清）
        String cacheKey = cache.notifyListKey(userId, isRead, typeTrim, pageNum, pageSize);
        CommonCacheService.CachedPage<NotificationVO> cached = cache.get(cacheKey,
                new TypeReference<CommonCacheService.CachedPage<NotificationVO>>() {
                });
        if (cached != null) {
            return R.success(cached.list()).add("total", cached.total());
        }
        // 2. 分页查询我的消息
        PageHelper.startPage(pageNum, pageSize);
        List<NotificationVO> list = notificationMapper.selectMyList(userId, isRead, typeTrim);
        PageInfo<NotificationVO> pageInfo = new PageInfo<>(list);
        // 2.5 写入缓存
        cache.put(cacheKey, new CommonCacheService.CachedPage<>(list, pageInfo.getTotal(),
                pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getPages()),
                CommonCacheService.TTL_SHORT);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public R unreadCount() {
        Long userId = currentUserId();
        if (userId == null) {
            return R.error("未获取到当前用户信息！");
        }
        // 0. 先查缓存（角标数，30 秒 TTL；已读操作会立即清）
        Long count = cache.get(cache.notifyUnreadKey(userId), Long.class);
        if (count == null) {
            count = notificationMapper.countUnread(userId);
            cache.put(cache.notifyUnreadKey(userId), count, CommonCacheService.TTL_SHORT);
        }
        return R.success(count);
    }

    @Override
    public R read(Long id) {
        // 1. 主键必填与当前用户
        if (id == null) {
            return R.error("消息ID不能为空！");
        }
        Long userId = currentUserId();
        if (userId == null) {
            return R.error("未获取到当前用户信息！");
        }
        // 2. 标记已读（WHERE 带接收人与未读守卫，防越权与重复标记）
        int rows = notificationMapper.updateRead(id, userId, LocalDateTime.now());
        if (rows == 0) {
            return R.error("消息不存在或已读！");
        }
        // 清该用户全部通知缓存（列表 + 未读角标立即新鲜）
        cache.evictPrefixAfterCommit(cache.notifyUserPrefix(userId));
        return R.success("标记已读成功！");
    }

    @Override
    public R readAll() {
        Long userId = currentUserId();
        if (userId == null) {
            return R.error("未获取到当前用户信息！");
        }
        notificationMapper.updateReadAll(userId, LocalDateTime.now());
        cache.evictPrefixAfterCommit(cache.notifyUserPrefix(userId));
        return R.success("全部标记已读成功！");
    }

    @Override
    public R publish(NotificationPublishDTO dto) {
        // 1. 兜底空值校验
        if (dto == null || !StringUtils.hasText(dto.getTitle())) {
            return R.error("公告标题不能为空！");
        }
        if (!StringUtils.hasText(dto.getContent())) {
            return R.error("公告内容不能为空！");
        }
        // 2. 获取全部有效用户（无接收人时提前返回）
        List<Long> userIds = sysUserMapper.selectActiveIds();
        if (userIds.isEmpty()) {
            return R.error("没有可接收的用户！");
        }
        // 3. 批量广播系统公告
        insertToUsers(userIds, null, dto.getTitle().trim(), dto.getContent().trim(),
                NotificationConstants.TYPE_SYSTEM_NOTICE);
        log.info("【通知】公告发布成功：title={}，接收人数={}", dto.getTitle(), userIds.size());
        // 广播公告涉及全部用户，清所有通知缓存
        cache.evictPrefixAfterCommit(CommonCacheService.NOTIFY_PREFIX);
        return R.success("公告发布成功！");
    }

    @Override
    public R manageList(Long userId, String type, Integer pageNum, Integer pageSize) {
        // 1. 分页参数与筛选条件
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        String typeTrim = StringUtils.hasText(type) ? type.trim() : null;
        // 2. 分页查询全部消息记录
        PageHelper.startPage(pageNum, pageSize);
        List<NotificationVO> list = notificationMapper.selectManageList(userId, typeTrim);
        PageInfo<NotificationVO> pageInfo = new PageInfo<>(list);
        return R.success(list).add("total", pageInfo.getTotal());
    }

    @Override
    public void send(Long userId, Long expressId, String title, String content, String type) {
        if (!enabled() || userId == null) {
            return;
        }
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            log.warn("【通知】标题或内容为空，跳过发送：type={}，userId={}", type, userId);
            return;
        }
        Notification notification = build(userId, expressId, title.trim(), content.trim(), type);
        notificationMapper.insert(notification);
        // 新消息进该用户收件箱，清其通知缓存（随外层事务提交）
        cache.evictPrefixAfterCommit(cache.notifyUserPrefix(userId));
    }

    @Override
    public void sendToAdmins(Long expressId, String title, String content, String type) {
        if (!enabled()) {
            return;
        }
        List<Long> userIds = sysUserMapper.selectActiveIdsByRole(RoleConstants.ADMIN);
        if (userIds.isEmpty()) {
            return;
        }
        insertToUsers(userIds, expressId, title, content, type);
        // 管理员数量少，直接清全部通知缓存（实现简单可靠）
        cache.evictPrefixAfterCommit(CommonCacheService.NOTIFY_PREFIX);
    }

    @Override
    public void broadcast(Long expressId, String title, String content, String type) {
        if (!enabled()) {
            return;
        }
        List<Long> userIds = sysUserMapper.selectActiveIds();
        if (userIds.isEmpty()) {
            return;
        }
        insertToUsers(userIds, expressId, title, content, type);
        // 广播涉及全部用户，清所有通知缓存
        cache.evictPrefixAfterCommit(CommonCacheService.NOTIFY_PREFIX);
    }

    /**
     * 按分片批量写入消息（参数守卫：开关/标题/内容为空时不写）
     * @param userIds 接收用户ID列表
     * @param expressId 关联快递ID，可空
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    private void insertToUsers(List<Long> userIds, Long expressId, String title, String content, String type) {
        if (!enabled() || !StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            log.warn("【通知】通知开关关闭或参数为空，跳过批量发送：type={}", type);
            return;
        }
        String titleTrim = title.trim();
        String contentTrim = content.trim();
        List<Notification> batch = new ArrayList<>(Math.min(userIds.size(), BATCH_SIZE));
        for (Long userId : userIds) {
            batch.add(build(userId, expressId, titleTrim, contentTrim, type));
            if (batch.size() >= BATCH_SIZE) {
                notificationMapper.insertBatch(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            notificationMapper.insertBatch(batch);
        }
    }

    /**
     * 组装消息实体（未读状态由数据库默认 0）
     * @param userId 接收用户ID
     * @param expressId 关联快递ID，可空
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     * @return 消息实体
     */
    private Notification build(Long userId, Long expressId, String title, String content, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setExpressId(expressId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        return notification;
    }

    /**
     * 读取通知开关（配置缺失视为开启）
     * @return true 允许发送
     */
    private boolean enabled() {
        String value = sysConfigService.getValueByKey(ConfigConstants.KEY_NOTIFICATION_ENABLED);
        return value == null || "true".equalsIgnoreCase(value.trim());
    }

    /**
     * 解析当前登录用户ID
     * @return 用户ID，未登录返回 null
     */
    private Long currentUserId() {
        String currentId = SecurityUtils.getCurrentUserId();
        return StringUtils.hasText(currentId) ? Long.valueOf(currentId) : null;
    }
}
