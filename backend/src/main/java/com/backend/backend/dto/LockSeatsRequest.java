package com.backend.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
// Remove the old import if it exists
// import com.backend.backend.service.SeatLockService; 
import com.backend.backend.dto.SeatIdentifier; // Import the new external DTO
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "锁定座位请求")
public class LockSeatsRequest {

    @NotEmpty(message = "需要锁定的座位列表不能为空")
    @Valid // Add validation to the list items
    @Schema(description = "需要锁定的座位标识符列表", required = true)
    private List<SeatIdentifier> seatsToLock; // Use the new external SeatIdentifier

    // 可以考虑添加其他字段，例如前端生成的锁定批次ID等，如果需要的话
} 