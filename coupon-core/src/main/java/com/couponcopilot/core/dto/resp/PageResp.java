package com.couponcopilot.core.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用分页响应对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用分页响应")
public class PageResp<T> {

    @Schema(description = "当前页码")
    private Long current;

    @Schema(description = "每页条数")
    private Long size;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页数据列表")
    private List<T> records;

    public static <T> PageResp<T> of(long current, long size, long total, List<T> records) {
        return PageResp.<T>builder()
                .current(current)
                .size(size)
                .total(total)
                .records(records)
                .build();
    }
}
