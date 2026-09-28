package com.example.station.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息通知实体（对应数据库表 notification，无逻辑删除列）
 * 消息类型见 NotificationConstants；isRead：0 未读，1 已读
 */
@Data
public class Notification {

    private Long id;                // 主键
    private Long userId;            // 接收用户 ID
    private Long expressId;         // 关联快递 ID（选填）
    private String title;           // 消息标题
    private String content;         // 消息内容
    private String type;            // 消息类型：PICKUP_NOTICE/OVERDUE_NOTICE/EXCEPTION_NOTICE/PICKUP_SUCCESS/SYSTEM_NOTICE
    private Integer isRead;         // 是否已读：0 未读，1 已读
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime readTime;     // 阅读时间
}
