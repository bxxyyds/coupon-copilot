package com.couponcopilot.common.web;

import com.couponcopilot.common.errorcode.BaseErrorCode;
import com.couponcopilot.common.exception.ClientException;
import com.couponcopilot.common.exception.ServiceException;
import com.couponcopilot.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        if (validator != null) {
            validator.close();
        }
    }

    @Test
    void shouldReturnSuccessWithTraceIdAndNoExtraSuccessField() throws Exception {
        MDC.put(Result.TRACE_ID_MDC_KEY, "trace-123");

        mockMvc.perform(get("/test/ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data").value("hello"))
                .andExpect(jsonPath("$.traceId").value("trace-123"))
                .andExpect(jsonPath("$.success").doesNotExist());
    }

    @Test
    void shouldHandleClientExceptionWithSpecificErrorCode() throws Exception {
        mockMvc.perform(get("/test/client-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000101"))
                .andExpect(jsonPath("$.message").value("时间参数非法"));
    }

    @Test
    void shouldHandleServiceExceptionWithCustomMessage() throws Exception {
        mockMvc.perform(get("/test/service-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B000201"))
                .andExpect(jsonPath("$.message").value("库存已抢光"));
    }

    @Test
    void shouldReturnReadableFieldErrorsWhenValidationFails() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"stock\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("name"),
                        containsString("stock")
                )));
    }

    @Test
    void shouldHideSensitiveDetailsWhenUnexpectedExceptionOccurs() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("B000001"))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后再试"))
                .andExpect(content().string(not(containsString("secret_db_password_leak"))));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/ok")
        public Result<String> ok() {
            return Result.success("hello");
        }

        @GetMapping("/test/client-error")
        public Result<Void> clientError() {
            throw new ClientException(BaseErrorCode.INVALID_TIME_RANGE);
        }

        @GetMapping("/test/service-error")
        public Result<Void> serviceError() {
            throw new ServiceException("库存已抢光", BaseErrorCode.COUPON_STOCK_NOT_ENOUGH);
        }

        @PostMapping("/test/validate")
        public Result<String> validate(@Valid @RequestBody CreateReq req) {
            return Result.success(req.getName());
        }

        @GetMapping("/test/unexpected-error")
        public Result<Void> unexpectedError() {
            throw new RuntimeException("secret_db_password_leak");
        }
    }

    @Data
    static class CreateReq {
        @NotBlank(message = "不能为空")
        private String name;

        @Min(value = 1, message = "必须大于0")
        private Integer stock;
    }
}
