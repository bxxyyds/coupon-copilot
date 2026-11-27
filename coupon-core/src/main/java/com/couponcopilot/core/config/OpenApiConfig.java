package com.couponcopilot.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 基础信息与全局调试请求头配置。
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

    /**
     * 为所有接口自动补充可选的身份请求头，方便在 Knife4j 单接口调试页直接填写与勾选。
     */
    @Bean
    public OperationCustomizer globalIdentityHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            addHeaderIfAbsent(operation, "X-Role", "调用方角色：MERCHANT(B端商家) / CUSTOMER(C端买家)", "MERCHANT");
            addHeaderIfAbsent(operation, "X-Shop-Id", "店铺ID（B端店铺券操作必填）", "1001");
            addHeaderIfAbsent(operation, "X-User-Id", "用户ID（C端操作或B端运营ID）", "2001");
            return operation;
        };
    }

    private void addHeaderIfAbsent(Operation operation, String name, String description, String example) {
        boolean exists = operation.getParameters() != null
                && operation.getParameters().stream().anyMatch(p -> name.equalsIgnoreCase(p.getName()));
        if (exists) {
            return;
        }
        operation.addParametersItem(new Parameter()
                .in("header")
                .name(name)
                .description(description)
                .required(false)
                .schema(new StringSchema())
                .example(example));
    }
}
