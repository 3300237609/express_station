package com.example.station.controller;

import com.example.station.common.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共控制层
 * 处理系统级通用 HTTP 请求（健康检查等）
 * 基础路径：/
 */
@RestController
public class HealthController {

    /**
     * 服务健康检查（无需登录，白名单接口）
     * 请求方式：GET
     * 请求路径：/health
     * 返回值：服务运行状态
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.success("服务运行正常！");
    }
}
