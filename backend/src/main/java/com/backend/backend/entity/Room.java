package com.backend.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cinemaId; // 所属影院ID

    private String name; // 影厅名称 (如 1号厅)

    @JsonProperty("rows")
    private Integer rowsCount; // 座位行数

    @JsonProperty("cols")
    private Integer colsCount; // 座位列数

    private String seatTemplate; // 座位模板 (JSON 格式)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
} 