package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotNull(message = "场次ID不能为空")
    @Schema(description = "场次 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long screeningId;

    @NotEmpty(message = "必须选择至少一个座位")
    @Valid // Enable validation for items in the list
    @Schema(description = "选择的座位列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SeatInfo> selectedSeats;

    // 内部类，表示一个座位的信息
    @Data
    @Schema(description = "座位信息")
    public static class SeatInfo {
        @NotNull(message = "行号不能为空")
        @Min(value = 1, message = "行号必须大于0")
        @Schema(description = "座位行号 (从1开始)", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
        private Integer rowIndex;

        @NotNull(message = "列号不能为空")
        @Min(value = 1, message = "列号必须大于0")
        @Schema(description = "座位列号 (从1开始)", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
        private Integer colIndex;
    }
} 