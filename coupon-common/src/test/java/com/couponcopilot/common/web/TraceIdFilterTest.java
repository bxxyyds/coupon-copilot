package com.couponcopilot.common.web;

import com.couponcopilot.common.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TraceIdFilterTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TraceTestController())
                .addFilters(new TraceIdFilter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldGenerateTraceIdAndClearMdcAfterRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/trace/ping"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, hasLength(16)))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.traceId", hasLength(16)))
                .andReturn();

        String headerTraceId = result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER);
        assertNotNull(headerTraceId);
        assertNull(MDC.get(Result.TRACE_ID_MDC_KEY));
    }

    @Test
    void shouldPropagateIncomingTraceIdAndClearMdc() throws Exception {
        mockMvc.perform(get("/trace/ping")
                        .header(TraceIdFilter.TRACE_ID_HEADER, "req-abc-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, "req-abc-001"))
                .andExpect(jsonPath("$.traceId").value("req-abc-001"))
                .andExpect(jsonPath("$.data").value("req-abc-001"));

        assertNull(MDC.get(Result.TRACE_ID_MDC_KEY));
    }

    @Test
    void shouldIncludeTraceIdOnErrorAndStillClearMdc() throws Exception {
        MvcResult result = mockMvc.perform(get("/trace/boom"))
                .andExpect(status().isOk())
                .andExpect(header().string(TraceIdFilter.TRACE_ID_HEADER, not(emptyOrNullString())))
                .andExpect(jsonPath("$.code").value("B000001"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())))
                .andReturn();

        assertEquals(16, result.getResponse().getHeader(TraceIdFilter.TRACE_ID_HEADER).length());
        assertNull(MDC.get(Result.TRACE_ID_MDC_KEY));
    }

    @RestController
    static class TraceTestController {

        @GetMapping("/trace/ping")
        public Result<String> ping() {
            return Result.success(MDC.get(Result.TRACE_ID_MDC_KEY));
        }

        @GetMapping("/trace/boom")
        public Result<Void> boom() {
            throw new RuntimeException("unexpected boom");
        }
    }
}
