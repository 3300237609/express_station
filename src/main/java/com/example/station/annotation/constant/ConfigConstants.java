package com.example.station.annotation.constant;

/**
 * 系统配置业务常量（config_key 字面量集中管理，与 config_data.sql 种子一致）
 */
public final class ConfigConstants {

    private ConfigConstants() {
    }

    /** 驿站名称 */
    public static final String KEY_STATION_NAME = "station.name";

    /** 超期提醒时长（小时） */
    public static final String KEY_OVERDUE_HOURS = "station.overdue.hours";

    /** 取件码长度 */
    public static final String KEY_PICKUP_CODE_LENGTH = "pickup.code.length";

    /** 是否开启消息通知 */
    public static final String KEY_NOTIFICATION_ENABLED = "notification.enabled";

    /** 超期时长兜底值（配置缺失/非法时使用，单位小时） */
    public static final int DEFAULT_OVERDUE_HOURS = 72;
}
