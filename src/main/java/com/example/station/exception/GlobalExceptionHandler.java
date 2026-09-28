package com.example.station.exception;

import com.example.station.common.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理器
 * 兜底将异常转换为统一 JSON 响应 { code, msg, data, map }
 * 注意：业务错误优先在 Service 中 return R.error(...) 提前返回，不依赖异常做流程控制
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 请求体参数校验失败（@Valid + DTO）：返回第一条校验失败消息
     * @param e 校验异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败！");
        return R.error(msg);
    }

    /**
     * 路径/查询参数校验失败（@Validated）
     * @param e 约束违反异常
     * @return 统一失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        return R.error(e.getMessage());
    }

    /**
     * 缺少必要请求参数
     * @param e 缺参异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.error("缺少必要参数：" + e.getParameterName() + "！");
    }

    /**
     * 请求参数类型不匹配
     * @param e 类型不匹配异常
     * @return 统一失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.error("参数类型错误：" + e.getName() + "！");
    }

    /**
     * 请求体格式错误（JSON 解析失败）
     * @param e 消息不可读异常
     * @return 统一失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.error("请求体格式错误！");
    }

    /**
     * 数据库唯一键冲突兜底（查重与唯一键已保持一致，此处防并发或遗漏场景）
     * @param e 重复键异常
     * @return 统一失败响应
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突：{}", e.getMessage());
        return R.error("数据已存在，请勿重复提交！");
    }

    /**
     * 兜底处理：未知系统异常记录日志并返回通用提示，避免堆栈外泄
     * @param e 任意异常
     * @return 统一失败响应
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统未知异常：", e);
        return R.error("系统繁忙，请稍后重试！");
    }
}
