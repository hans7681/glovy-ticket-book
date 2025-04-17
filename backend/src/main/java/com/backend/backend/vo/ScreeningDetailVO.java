package com.backend.backend.vo;

// Import Screening entity to use its inner enum
import com.backend.backend.entity.Screening;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "场次详情视图对象，包含电影、影院、影厅的详细信息")
public class ScreeningDetailVO {

    @Schema(description = "场次 ID", example = "1")
    private Long screeningId;

    @Schema(description = "电影 ID", example = "101")
    private Long movieId;

    @Schema(description = "电影标题", example = "挑战者号")
    private String movieTitle;

    @Schema(description = "电影海报 URL", example = "/images/posters/challenger.jpg")
    private String moviePosterUrl; // 注意：Movie 实体中可能需要有 posterUrl 字段

    @Schema(description = "(可选) 电影时长 (分钟)", example = "131")
    private Integer movieDuration; // 注意：Movie 实体中可能需要有 duration 字段

    @Schema(description = "影院 ID", example = "5")
    private Long cinemaId;

    @Schema(description = "影院名称", example = "星光国际影城")
    private String cinemaName;

    @Schema(description = "影厅 ID", example = "12")
    private Long roomId;

    @Schema(description = "影厅名称", example = "IMAX 厅")
    private String roomName;

    @Schema(description = "放映开始时间", example = "2024-08-15T14:30:00")
    private LocalDateTime startTime;

    @Schema(description = "放映结束时间", example = "2024-08-15T16:41:00")
    private LocalDateTime endTime;

    @Schema(description = "票价", example = "55.00")
    private BigDecimal price;

    @Schema(description = "场次状态", example = "APPROVED")
    private Screening.ScreeningStatus status;

    // 可以根据需要添加其他字段，例如影厅的行数和列数
    @Schema(description = "(可选) 影厅行数", example = "10")
    private Integer roomRowsCount;

    @Schema(description = "(可选) 影厅列数", example = "20")
    private Integer roomColsCount;
} 