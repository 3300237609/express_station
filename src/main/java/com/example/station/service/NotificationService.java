package com.example.station.service;

import com.example.station.common.R;
import com.example.station.dto.NotificationPublishDTO;
import org.springframework.validation.annotation.Validated;

/**
 * 消息通知业务接口（含事件通知内部发送能力：入库/取件/异常/超期等场景调用）
 */
@Validated
public interface NotificationService {

    /**
     * 分页查询我的消息（按接收人过滤，可按已读状态/类型筛选，按创建时间倒序）
     * @param isRead 已读状态：0 未读，1 已读，可空
     * @param type 消息类型，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页消息列表，附 total 总条数
     */
    R list(Integer isRead, String type, Integer pageNum, Integer pageSize);

    /**
     * 查询我的未读消息数量（消息角标用）
     * @return 未读条数
     */
    R unreadCount();

    /**
     * 标记单条消息已读（接收人校验防越权）
     * @param id 消息ID
     * @return 标记结果
     */
    R read(Long id);

    /**
     * 全部标记已读（仅我的未读消息）
     * @return 标记结果
     */
    R readAll();

    /**
     * 发布系统公告（管理员向全部有效用户广播 SYSTEM_NOTICE）
     * @param dto 公告入参（标题、内容）
     * @return 发布结果
     */
    R publish(@Validated NotificationPublishDTO dto);

    /**
     * 分页查询全部消息记录（管理员查看通知记录，可按接收人/类型筛选）
     * @param userId 接收用户ID，可空
     * @param type 消息类型，可空
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页消息列表，附 total 总条数
     */
    R manageList(Long userId, String type, Integer pageNum, Integer pageSize);

    /**
     * 发送单条通知（事件通知内部使用；通知开关关闭、接收人为空或标题/内容为空时不发送）
     * @param userId 接收用户ID
     * @param expressId 关联快递ID，可空
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    void send(Long userId, Long expressId, String title, String content, String type);

    /**
     * 向全部有效管理员发送通知（事件通知内部使用，开关与参数守卫同 send）
     * @param expressId 关联快递ID，可空
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    void sendToAdmins(Long expressId, String title, String content, String type);

    /**
     * 向全部有效用户广播通知（事件通知内部使用，开关与参数守卫同 send）
     * @param expressId 关联快递ID，可空
     * @param title 消息标题
     * @param content 消息内容
     * @param type 消息类型
     */
    void broadcast(Long expressId, String title, String content, String type);
}
