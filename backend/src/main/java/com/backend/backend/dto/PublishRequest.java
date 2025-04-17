package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PublishRequest {
    @Schema(description = "是否发布", required = true)
    private boolean publish;
} 