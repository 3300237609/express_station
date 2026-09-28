package com.example.station.vo;

import lombok.Data;

/**
 * 消息通知出参（LEFT JOIN 关联快递单号，时间 DATE_FORMAT 转字符串）
 */
@Data
public class NotificationVO {

    private Long id;            // 消息ID
    private Long userId;        // 接收用户ID
    private Long expressId;     // 关联快递ID
    private String expressNo;   // 关联快递单号（无关联为空）
    private String title;       // 消息标题
    private String content;     // 消息内容
    private String type;        // 消息类型
    private Integer isRead;     // 是否已读：0 未读，1 已读
    private String createTime;  // 创建时间
    private String readTime;    // 阅读时间
}
