package com.couponcopilot.core.controller;

import com.couponcopilot.common.web.CommonWebConfig;
import com.couponcopilot.common.web.GlobalExceptionHandler;
import com.couponcopilot.common.web.TraceIdFilter;
import com.couponcopilot.common.web.UserContextInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HealthController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, UserContextInterceptor.class, CommonWebConfig.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void healthShouldReturnUpWithTraceId() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        MvcResult result = mockMvc.perform(get("/api/coupon/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.service").value("coupon-core"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.database").value("UP"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())))
                .andReturn();

        String headerTraceId = result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertFalse(headerTraceId == null || headerTraceId.isBlank());
        String body = result.getResponse().getContentAsString();
        assertEquals(true, body.contains("\"traceId\":\"" + headerTraceId + "\""));
    }

    @Test
    void healthShouldReturnServiceErrorWhenDatabaseFails() throws Exception {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("connection refused"));

        mockMvc.perform(get("/api/coupon/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.code").value("B000001"))
                .andExpect(jsonPath("$.message").value("数据库连接异常"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));
    }

    @Test
    void healthShouldRejectInvalidUserIdHeader() throws Exception {
        mockMvc.perform(get("/api/coupon/health").header("X-User-Id", "abc"))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.code").value("A000001"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));
    }
}
