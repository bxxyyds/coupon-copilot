package com.couponcopilot.common.web;

import com.couponcopilot.common.context.UserContext;
import com.couponcopilot.common.context.UserRole;
import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ServiceException;
import com.couponcopilot.common.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserContextInterceptorTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ContextTestController())
                .addFilters(new TraceIdFilter())
                .addInterceptors(new UserContextInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        MDC.clear();
    }

    @Test
    void shouldPopulateUserContextAndClearAfterCompletion() throws Exception {
        mockMvc.perform(get("/ctx/info")
                        .header(UserContextInterceptor.HEADER_ROLE, "merchant")
                        .header(UserContextInterceptor.HEADER_USER_ID, "1001")
                        .header(UserContextInterceptor.HEADER_SHOP_ID, "2001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value("MERCHANT:1001:2001"));

        assertNull(UserContext.get());
        assertNull(MDC.get(Result.TRACE_ID_MDC_KEY));
    }

    @Test
    void shouldAllowRequestsWithoutIdentityHeaders() throws Exception {
        mockMvc.perform(get("/ctx/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data").value("ok"));

        assertNull(UserContext.get());
    }

    @Test
    void shouldRejectNonNumericUserIdWithTraceId() throws Exception {
        mockMvc.perform(get("/ctx/info")
                        .header(UserContextInterceptor.HEADER_ROLE, "CUSTOMER")
                        .header(UserContextInterceptor.HEADER_USER_ID, "not-a-number"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));

        assertNull(UserContext.get());
    }

    @Test
    void shouldRejectInvalidRoleHeader() throws Exception {
        mockMvc.perform(get("/ctx/info")
                        .header(UserContextInterceptor.HEADER_ROLE, "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"))
                .andExpect(jsonPath("$.traceId", not(emptyOrNullString())));

        assertNull(UserContext.get());
    }

    @Test
    void shouldClearUserContextEvenWhenControllerThrowsException() throws Exception {
        mockMvc.perform(get("/ctx/fail")
                        .header(UserContextInterceptor.HEADER_ROLE, "CUSTOMER")
                        .header(UserContextInterceptor.HEADER_USER_ID, "1002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B000201"));

        assertNull(UserContext.get());
        assertNull(MDC.get(Result.TRACE_ID_MDC_KEY));
    }

    @RestController
    static class ContextTestController {

        @GetMapping("/ctx/info")
        public Result<String> info() {
            UserRole role = UserContext.getRole();
            return Result.success(role + ":" + UserContext.getUserId() + ":" + UserContext.getShopId());
        }

        @GetMapping("/ctx/health")
        public Result<String> health() {
            return Result.success("ok");
        }

        @GetMapping("/ctx/fail")
        public Result<Void> fail() {
            throw new ServiceException(BaseErrorCode.COUPON_STOCK_NOT_ENOUGH);
        }
    }
}
