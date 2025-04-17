package com.backend.backend.dto;

import com.backend.backend.entity.Cinema;
import lombok.Data;
 
@Data
public class CinemaStatusUpdateRequest {
    private Cinema.CinemaStatus status; // 新的影院状态
} 