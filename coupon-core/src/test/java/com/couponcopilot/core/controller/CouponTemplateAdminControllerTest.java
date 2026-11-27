package com.couponcopilot.core.controller;

import com.couponcopilot.common.web.CommonWebConfig;
import com.couponcopilot.common.web.GlobalExceptionHandler;
import com.couponcopilot.common.web.TraceIdFilter;
import com.couponcopilot.common.web.UserContextInterceptor;
import com.couponcopilot.core.dto.resp.CouponTemplateCreateResp;
import com.couponcopilot.core.dto.resp.CouponTemplateResp;
import com.couponcopilot.core.dto.resp.PageResp;
import com.couponcopilot.core.service.CouponTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponTemplateAdminController.class)
@Import({GlobalExceptionHandler.class, TraceIdFilter.class, UserContextInterceptor.class, CommonWebConfig.class})
class CouponTemplateAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponTemplateService couponTemplateService;

    @Test
    void shouldCreateTemplateAndSerializeLongIdAsString() throws Exception {
        when(couponTemplateService.createTemplate(any())).thenReturn(CouponTemplateCreateResp.builder()
                .id(1890123456789012345L)
                .shopId(1001L)
                .name("满200减30")
                .status(0)
                .stock(500)
                .build());

        String body = """
                {
                  "name": "满200减30",
                  "source": 0,
                  "target": 1,
                  "type": 1,
                  "validStartTime": "2026-12-01 00:00:00",
                  "validEndTime": "2026-12-31 23:59:59",
                  "stock": 500,
                  "receiveRule": "{\\"limitPerPerson\\":2}",
                  "consumeRule": "{\\"termsOfUse\\":200,\\"maximumDiscountAmount\\":30}"
                }
                """;

        mockMvc.perform(post("/api/coupon/admin/template/create")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists(TraceIdFilter.TRACE_ID_HEADER))
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.id").value("1890123456789012345"))
                .andExpect(jsonPath("$.data.shopId").value("1001"))
                .andExpect(jsonPath("$.data.status").value(0))
                .andExpect(jsonPath("$.data.stock").value(500));
    }

    @Test
    void shouldRejectValidationFailureWithClientError() throws Exception {
        String invalidBody = """
                {
                  "name": "",
                  "source": 0,
                  "target": 1,
                  "type": 1,
                  "validStartTime": "2026-12-01 00:00:00",
                  "validEndTime": "2026-12-31 23:59:59",
                  "stock": 0,
                  "receiveRule": "{\\"limitPerPerson\\":2}",
                  "consumeRule": "{\\"termsOfUse\\":200}"
                }
                """;

        mockMvc.perform(post("/api/coupon/admin/template/create")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"))
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("name"),
                        containsString("stock")
                )));

        verify(couponTemplateService, never()).createTemplate(any());
    }

    @Test
    void shouldRejectBadDateFormatWithRequestBodyError() throws Exception {
        String badDateBody = """
                {
                  "name": "测试券",
                  "source": 0,
                  "target": 1,
                  "type": 1,
                  "validStartTime": "2026/12/01",
                  "validEndTime": "2026-12-31 23:59:59",
                  "stock": 10,
                  "receiveRule": "{\\"limitPerPerson\\":2}",
                  "consumeRule": "{\\"termsOfUse\\":200}"
                }
                """;

        mockMvc.perform(post("/api/coupon/admin/template/create")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badDateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000100"));

        verify(couponTemplateService, never()).createTemplate(any());
    }

    @Test
    void shouldPageTemplatesAndSerializeIdsAsStrings() throws Exception {
        CouponTemplateResp item = CouponTemplateResp.builder()
                .id(1890123456789012345L)
                .shopId(1001L)
                .name("低库存券")
                .stock(20)
                .status(0)
                .build();
        when(couponTemplateService.pageTemplates(any()))
                .thenReturn(PageResp.of(1L, 10L, 1L, List.of(item)));

        mockMvc.perform(get("/api/coupon/admin/template/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("maxStock", "100")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value("1890123456789012345"))
                .andExpect(jsonPath("$.data.records[0].stock").value(20));
    }

    @Test
    void shouldRejectInvalidPageSizeAndZeroIncreaseDelta() throws Exception {
        mockMvc.perform(get("/api/coupon/admin/template/page")
                        .param("size", "200")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"));

        verify(couponTemplateService, never()).pageTemplates(any());

        mockMvc.perform(post("/api/coupon/admin/template/increase-stock")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":100,\"delta\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("A000001"));

        verify(couponTemplateService, never()).increaseStock(any());
    }

    @Test
    void shouldTerminateTemplateSuccessfully() throws Exception {
        mockMvc.perform(post("/api/coupon/admin/template/terminate")
                        .header("X-Role", "MERCHANT")
                        .header("X-Shop-Id", "1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1890123456789012345}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        verify(couponTemplateService).terminateTemplate(any());
    }
}
