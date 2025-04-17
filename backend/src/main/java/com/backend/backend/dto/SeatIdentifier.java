package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 用于标识座位的简单数据传输对象 (行号和列号)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "座位标识符，包含行号和列号")
public class SeatIdentifier {

    @NotNull(message = "行号不能为空")
    @Min(value = 1, message = "行号必须大于0")
    @Schema(description = "座位行号 (从1开始)", required = true, example = "5")
    private Integer rowIndex;

    @NotNull(message = "列号不能为空")
    @Min(value = 1, message = "列号必须大于0")
    @Schema(description = "座位列号 (从1开始)", required = true, example = "8")
    private Integer colIndex;
} 