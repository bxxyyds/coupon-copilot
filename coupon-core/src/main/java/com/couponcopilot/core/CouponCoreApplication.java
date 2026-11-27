package com.couponcopilot.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 卡券核心服务启动类。
 * 显式扫描 com.couponcopilot 根包，确保 coupon-common 中的过滤器、拦截器与全局异常处理生效。
 */
@SpringBootApplication(scanBasePackages = "com.couponcopilot")
public class CouponCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponCoreApplication.class, args);
    }
}
