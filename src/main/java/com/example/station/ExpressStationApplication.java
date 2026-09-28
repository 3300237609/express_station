package com.example.station;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 快递末端智能驿站系统 启动类
 * 单体应用，Mapper 接口统一加 @Mapper 注解由 MyBatis 自动扫描
 * @EnableScheduling：启用超期快递定时检测（OverdueTask）
 */
@SpringBootApplication
@EnableScheduling
public class ExpressStationApplication {

    /**
     * 应用入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ExpressStationApplication.class, args);
    }
}
