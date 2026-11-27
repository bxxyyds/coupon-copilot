package com.couponcopilot.core.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 健康检查响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康检查响应")
public class HealthResp {

    @Schema(description = "服务名称", example = "coupon-core")
    private String service;

    @Schema(description = "服务状态", example = "UP")
    private String status;

    @Schema(description = "数据库状态", example = "UP")
    private String database;

    @Schema(description = "服务器当前时间")
    private LocalDateTime serverTime;
}
