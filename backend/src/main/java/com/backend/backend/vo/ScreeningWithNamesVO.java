package com.backend.backend.vo;

import com.backend.backend.entity.Screening;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 包含影院和影厅名称的场次视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true) // 如果需要比较基类字段
public class ScreeningWithNamesVO extends Screening { // 可以继承 Screening 以复用字段，或单独定义所有字段

    private String cinemaName;
    private String roomName;

    // 可以添加一个静态工厂方法或构造函数，方便从 Screening 转换
    public static ScreeningWithNamesVO fromScreening(Screening screening, String cinemaName, String roomName) {
        ScreeningWithNamesVO vo = new ScreeningWithNamesVO();
        // 复制 Screening 的所有属性到 VO
        vo.setId(screening.getId());
        vo.setMovieId(screening.getMovieId());
        vo.setRoomId(screening.getRoomId());
        vo.setCinemaId(screening.getCinemaId());
        vo.setStartTime(screening.getStartTime());
        vo.setEndTime(screening.getEndTime());
        vo.setPrice(screening.getPrice());
        vo.setStatus(screening.getStatus());
        vo.setCreateTime(screening.getCreateTime());
        vo.setUpdateTime(screening.getUpdateTime());

        // 设置额外的名称属性
        vo.setCinemaName(cinemaName);
        vo.setRoomName(roomName);
        return vo;
    }
} 