package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "座位信息")
public class SeatInfo {

    @NotNull(message = "行号不能为空")
    @Min(value = 1, message = "行号必须大于0")
    @Schema(description = "座位行号 (从1开始)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer rowIndex;

    @NotNull(message = "列号不能为空")
    @Min(value = 1, message = "列号必须大于0")
    @Schema(description = "座位列号 (从1开始)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer colIndex;
} 