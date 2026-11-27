package com.couponcopilot.core.controller;

import com.couponcopilot.common.exception.ServiceException;
import com.couponcopilot.common.result.Result;
import com.couponcopilot.core.dto.resp.HealthResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 系统健康检查控制器。
 */
@Slf4j
@Tag(name = "系统健康检查")
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "健康检查", description = "验证服务运行状态、MySQL 连通性、统一 Result 返回结构与 TraceId 链路追踪")
    @GetMapping("/health")
    public Result<HealthResp> health() {
        try {
            Integer ping = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (ping == null || ping != 1) {
                throw new ServiceException("数据库连通性校验失败");
            }
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Health check database ping failed", ex);
            throw new ServiceException("数据库连接异常");
        }

        log.info("Health check passed");
        return Result.success(HealthResp.builder()
                .service("coupon-core")
                .status("UP")
                .database("UP")
                .serverTime(LocalDateTime.now())
                .build());
    }
}
