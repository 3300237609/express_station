package com.example.station.vo;

import lombok.Data;

/**
 * 异常件信息出参（异常记录 JOIN 快递单号与登记/处理人姓名，时间 DATE_FORMAT 转字符串）
 */
@Data
public class ExceptionVO {

    private Long id;                // 异常记录ID
    private Long expressId;         // 快递ID
    private String expressNo;       // 快递单号
    private String receiverName;    // 收件人
    private String receiverPhone;   // 收件人电话
    private String expressStatus;   // 快递当前状态
    private String exceptionType;   // 异常类型
    private String description;     // 异常描述
    private String imageUrl;        // 异常图片
    private Long reporterId;        // 登记人ID
    private String reporterName;    // 登记人姓名
    private Long handlerId;         // 处理人ID
    private String handlerName;     // 处理人姓名
    private String handleStatus;    // 处理状态
    private String handleRemark;    // 处理结果
    private String createTime;      // 登记时间
    private String handleTime;      // 处理时间
    private String updateTime;      // 更新时间
}
