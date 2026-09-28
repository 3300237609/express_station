package com.example.station.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * 标注在 Controller 写操作方法上，由 OperationLogAspect 自动记录到 operation_log 表
 * description 支持 SpEL 模板（如 "快递单号：#{#dto.expressNo}"），解析失败时按原文记录
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 操作模块（如：快递管理、快递入库、取件出库） */
    String module();

    /** 操作类型（如：新增快递、完成入库、确认取件） */
    String operation();

    /** 操作描述（支持 #{#参数名} SpEL 模板，可空） */
    String description() default "";

    /** 匿名场景备用操作人用户名（SpEL 模板如 "#{#dto.username}"，仅在无当前登录人且业务成功时反查用户落日志，可空） */
    String operator() default "";
}
