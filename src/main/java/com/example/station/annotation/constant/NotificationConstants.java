package com.example.station.annotation.constant;

/**
 * 消息通知业务常量（消息类型字面量集中管理）
 */
public final class NotificationConstants {

    private NotificationConstants() {
    }

    /** 快递入库/取件码生成通知 */
    public static final String TYPE_PICKUP_NOTICE = "PICKUP_NOTICE";

    /** 超期提醒通知 */
    public static final String TYPE_OVERDUE_NOTICE = "OVERDUE_NOTICE";

    /** 异常件通知（登记/处理完成） */
    public static final String TYPE_EXCEPTION_NOTICE = "EXCEPTION_NOTICE";

    /** 取件成功通知 */
    public static final String TYPE_PICKUP_SUCCESS = "PICKUP_SUCCESS";

    /** 系统公告 */
    public static final String TYPE_SYSTEM_NOTICE = "SYSTEM_NOTICE";
}
