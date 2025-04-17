package com.backend.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RatingRequestDTO {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分必须介于 1 到 10 之间")
    @Max(value = 10, message = "评分必须介于 1 到 10 之间")
    private Integer score; // 1-10

    private String comment; // 可选的评论内容
}
