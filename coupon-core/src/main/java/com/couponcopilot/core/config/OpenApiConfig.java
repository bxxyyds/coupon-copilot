package com.couponcopilot.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 基础信息配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI couponCoreOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("coupon-copilot 卡券核心服务 API")
                .description("高并发卡券核心服务接口文档。演示身份请求头：X-Role (MERCHANT/CUSTOMER)、X-User-Id、X-Shop-Id；链路追踪请求头：X-Trace-Id。")
                .version("v0.1.0"));
    }
}
