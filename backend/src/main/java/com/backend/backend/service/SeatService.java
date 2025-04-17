package com.backend.backend.service;

import com.backend.backend.vo.SeatStatusVO;

import java.util.List;

public interface SeatService {

    /**
     * 获取指定场次的座位图及状态
     * @param screeningId 场次ID
     * @return 座位状态视图对象列表，包含每个座位的行、列、状态
     * @throws RuntimeException 如果场次或影厅信息不存在
     */
    List<List<SeatStatusVO>> getSeatStatus(Long screeningId);

} 