package com.backend.backend.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScreeningRequest {
    private Long movieId;
    private Long roomId;
    private LocalDateTime startTime;
    private BigDecimal price;
} 