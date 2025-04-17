package com.backend.backend.dto;

import com.backend.backend.dto.SeatIdentifier;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import java.util.List;

@Data
@Schema(description = "解锁座位请求")
public class UnlockSeatsRequest {

    @NotEmpty(message = "需要解锁的座位列表不能为空")
    @Valid // 确保列表中的 SeatIdentifier 也被校验 (如果 SeatIdentifier 有约束)
    @Schema(description = "需要解锁的座位标识符列表", required = true)
    private List<SeatIdentifier> seatsToUnlock;
} 