package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通用图表数据点 DTO (名称/标签 和 值)")
public class ChartDataPointDTO {

    @Schema(description = "数据点的名称或标签 (例如日期 'YYYY-MM-DD' 或电影标题)")
    private String name;

    @Schema(description = "数据点的值 (例如订单数量或销售额)")
    private BigDecimal value; // 使用 BigDecimal 保证精度
}
