package com.example.station.aspect;

import com.example.station.annotation.OperationLog;
import com.example.station.common.R;
import com.example.station.mapper.OperationLogMapper;
import com.example.station.mapper.SysUserMapper;
import com.example.station.mapper.SysUserRoleMapper;
import com.example.station.utils.SecurityUtils;
import com.example.station.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 拦截标注了 @OperationLog 的 Controller 方法，执行后写入 operation_log 表：
 * 谁（操作人/角色）在什么时候执行了什么操作（模块/类型/描述）以及操作是否成功（SUCCESS/FAIL）
 * 日志写入失败只告警不中断业务
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 描述字段截断长度（与表列 VARCHAR(500) 对齐） */
    private static final int MAX_DESCRIPTION = 500;

    /** 请求地址截断长度（与表列 VARCHAR(500) 对齐） */
    private static final int MAX_URL = 500;

    /** IP 截断长度（与表列 VARCHAR(50) 对齐） */
    private static final int MAX_IP = 50;

    private final OperationLogMapper operationLogMapper;

    private final SysUserMapper sysUserMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * 环绕通知：执行业务方法后记录日志（业务异常与 R.error 均记为 FAIL）
     * @param joinPoint 连接点
     * @param opLog 方法上的操作日志注解
     * @return 业务方法返回值
     * @throws Throwable 业务方法抛出的异常（原样抛出）
     */
    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        Object result = null;
        String defaultResult = "SUCCESS";
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            defaultResult = "FAIL";
            throw e;
        } finally {
            try {
                record(joinPoint, opLog, result, defaultResult);
            } catch (Exception e) {
                log.warn("【日志】操作日志写入失败：{}-{}，原因：{}",
                        opLog.module(), opLog.operation(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * 组装并写入一条操作日志
     * @param joinPoint 连接点（取方法与参数、请求信息）
     * @param opLog 注解（模块/类型/描述模板）
     * @param result 业务返回值（R.code=0 记为 FAIL）
     * @param defaultResult 默认结果（异常路径为 FAIL）
     */
    private void record(ProceedingJoinPoint joinPoint, OperationLog opLog, Object result, String defaultResult) {
        // 1. 操作人：优先当前登录人；匿名场景（如注册）在业务成功后按 operator SpEL 解析用户名反查新用户，均取不到则跳过（user_id 为非空列）
        String currentId = SecurityUtils.getCurrentUserId();
        Long userId = null;
        com.example.station.entity.SysUser operatorUser = null;
        String roleCode = SecurityUtils.getCurrentRole();
        if (StringUtils.hasText(currentId)) {
            userId = Long.valueOf(currentId);
        } else if (StringUtils.hasText(opLog.operator())
                && result instanceof R<?> r && Integer.valueOf(1).equals(r.getCode())) {
            String operatorName = buildDescription(opLog.operator(), joinPoint);
            if (StringUtils.hasText(operatorName)) {
                operatorUser = sysUserMapper.selectByUsername(operatorName);
                if (operatorUser != null && operatorUser.getId() != null) {
                    userId = operatorUser.getId();
                    java.util.List<String> roleCodes = sysUserRoleMapper.selectRoleCodesByUserId(userId);
                    if (roleCodes != null && !roleCodes.isEmpty()) {
                        roleCode = roleCodes.get(0);
                    }
                }
            }
        }
        if (userId == null) {
            log.warn("【日志】未获取到当前用户，跳过记录：{}-{}", opLog.module(), opLog.operation());
            return;
        }
        com.example.station.entity.OperationLog entity = new com.example.station.entity.OperationLog();
        entity.setUserId(userId);
        UserVO user = sysUserMapper.selectById(userId);
        entity.setUsername(user != null && StringUtils.hasText(user.getUsername()) ? user.getUsername()
                : (operatorUser != null ? operatorUser.getUsername() : currentId));
        entity.setRoleCode(roleCode);
        // 2. 操作内容（支持 SpEL 模板，解析失败按原文记录）
        entity.setModule(opLog.module());
        entity.setOperation(opLog.operation());
        entity.setDescription(truncate(buildDescription(opLog.description(), joinPoint), MAX_DESCRIPTION));
        // 3. 请求信息
        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setRequestMethod(request.getMethod());
            entity.setRequestUrl(truncate(buildUrl(request), MAX_URL));
            entity.setIpAddress(truncate(resolveIp(request), MAX_IP));
        }
        // 4. 操作结果（业务返回 code=0 视为失败）
        String opResult = defaultResult;
        if (result instanceof R<?> r && r.getCode() == 0) {
            opResult = "FAIL";
        }
        entity.setResult(opResult);
        entity.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(entity);
    }

    /**
     * 解析描述模板：含 #{...} 时按 SpEL 模板求值，否则按原文返回
     * @param template 描述模板（如「快递单号：#{#dto.expressNo}」）
     * @param joinPoint 连接点（提供方法参数名与参数值）
     * @return 解析后的描述，入参为空返回 null
     */
    private String buildDescription(String template, ProceedingJoinPoint joinPoint) {
        if (!StringUtils.hasText(template)) {
            return template;
        }
        if (!template.contains("#{")) {
            return template;
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String[] names = parameterNameDiscoverer.getParameterNames(method);
            Object[] args = joinPoint.getArgs();
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (names != null) {
                for (int i = 0; i < names.length && i < args.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            return expressionParser.parseExpression(template, new TemplateParserContext())
                    .getValue(context, String.class);
        } catch (Exception e) {
            log.warn("【日志】描述表达式解析失败，按原文记录：{}，原因：{}", template, e.getMessage());
            return template;
        }
    }

    /**
     * 获取当前请求（非 Web 上下文返回 null）
     * @return HTTP 请求
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes ? servletAttributes.getRequest() : null;
    }

    /**
     * 拼接请求地址（路径 + 查询串）
     * @param request HTTP 请求
     * @return 请求地址
     */
    private String buildUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return StringUtils.hasText(query) ? uri + "?" + query : uri;
    }

    /**
     * 解析客户端 IP（优先 X-Forwarded-For 首段，否则取直连地址）
     * @param request HTTP 请求
     * @return IP 地址
     */
    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 字符串截断（超长保护）
     * @param text 原文
     * @param maxLength 最大长度
     * @return 截断后的字符串，入参为空返回 null
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
