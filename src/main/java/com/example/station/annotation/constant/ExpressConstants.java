package com.example.station.annotation.constant;

import java.util.Set;

/**
 * 快递业务常量（状态/尺寸字面量集中管理，SQL 与代码引用同一来源）
 */
public final class ExpressConstants {

    private ExpressConstants() {
    }

    /** 快递状态：待入库 */
    public static final String STATUS_WAITING = "WAITING";

    /** 快递状态：待取件（入库完成后的正常待取状态） */
    public static final String STATUS_WAITING_PICKUP = "WAITING_PICKUP";

    /** 快递状态：已取件（取件出库） */
    public static final String STATUS_PICKED_UP = "PICKED_UP";

    /** 快递状态：异常 */
    public static final String STATUS_EXCEPTION = "EXCEPTION";

    /** 快递状态：已入库（中间态，由管理员状态修正使用） */
    public static final String STATUS_STORED = "STORED";

    /** 快递状态：已退回 */
    public static final String STATUS_RETURNED = "RETURNED";

    /** 快递状态全集：WAITING/STORED/WAITING_PICKUP/PICKED_UP/EXCEPTION/RETURNED */
    public static final Set<String> STATUS_SET =
            Set.of("WAITING", "STORED", "WAITING_PICKUP", "PICKED_UP", "EXCEPTION", "RETURNED");

    /** 需要货位已释放才允许进入的状态（仍关联货位时禁止置为这些状态） */
    public static final Set<String> STATUS_NEED_NO_LOCATION =
            Set.of("PICKED_UP", "RETURNED");

    /** 尺寸类型全集：SMALL/MEDIUM/LARGE */
    public static final Set<String> SIZE_SET = Set.of("SMALL", "MEDIUM", "LARGE");

    /** 异常类型全集：DAMAGE/LOST/WRONG_ADDR/WRONG_INFO/REFUSED/OVERDUE/OTHER */
    public static final Set<String> EXCEPTION_TYPE_SET =
            Set.of("DAMAGE", "LOST", "WRONG_ADDR", "WRONG_INFO", "REFUSED", "OVERDUE", "OTHER");

    /** 异常处理状态：待处理（登记后的初始状态） */
    public static final String HANDLE_STATUS_PENDING = "PENDING";

    /** 异常处理状态全集：PENDING/PROCESSING/RESOLVED/CLOSED/REJECTED */
    public static final Set<String> HANDLE_STATUS_SET =
            Set.of("PENDING", "PROCESSING", "RESOLVED", "CLOSED", "REJECTED");

    /** 异常处理终态（进入终态时回填处理时间与处理人，并恢复快递状态） */
    public static final Set<String> HANDLE_STATUS_TERMINAL =
            Set.of("RESOLVED", "CLOSED", "REJECTED");

    /** 取件方式全集：CODE/SCAN/ADMIN/COURIER */
    public static final Set<String> PICKUP_METHOD_SET = Set.of("CODE", "SCAN", "ADMIN", "COURIER");
}
