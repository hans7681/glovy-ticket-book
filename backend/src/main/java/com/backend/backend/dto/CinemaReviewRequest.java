package com.backend.backend.dto;

import lombok.Data;
 
@Data
public class CinemaReviewRequest {
    private boolean approved; // 是否批准
    private Long adminUserId;   // 如果批准，关联的影院管理员 User ID (可选)
} 