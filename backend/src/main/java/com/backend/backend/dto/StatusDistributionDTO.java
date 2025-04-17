package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "状态分布数据点 DTO")
public class StatusDistributionDTO {

    @Schema(description = "状态名称 (如 'APPROVED', 'PENDING_APPROVAL')")
    private String status; // 或者使用枚举类型 String 表示

    @Schema(description = "该状态下的数量")
    private Long count;
}
