package com.backend.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatStatusVO {

    private int rowIndex;
    private int colIndex;
    private SeatState status;

    // 座位状态枚举
    public enum SeatState {
        AVAILABLE,  // 可用
        SOLD,       // 已售
        LOCKED,     // 已被锁定 (当前用户或其他用户)
        SELECTED,   // 当前用户已选择 (前端状态，后端通常返回 LOCKED)
        UNAVAILABLE // 不可用 (例如过道、损坏)
        // 注意：后端一般只区分 AVAILABLE, SOLD, LOCKED。UNAVAILABLE 可能需要从 Room 的 seatTemplate 获取。
        // SELECTED 是前端维护的状态。
    }
} 