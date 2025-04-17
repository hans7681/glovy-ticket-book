package com.backend.backend.service.impl;

import com.backend.backend.entity.Cinema;
import com.backend.backend.entity.Room;
import com.backend.backend.mapper.RoomMapper;
import com.backend.backend.service.CinemaService;
import com.backend.backend.service.RoomService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    @Autowired
    private CinemaService cinemaService;

    /**
     * 校验影院管理员是否有权限操作指定的影厅或影院
     * @param cinemaId 影院ID
     * @param adminUserId 影院管理员用户ID
     * @return 影院对象，如果无权限则抛出异常
     */
    private Cinema checkCinemaAdminPermission(Long cinemaId, Long adminUserId) {
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);
        if (cinema == null || !cinema.getId().equals(cinemaId) || cinema.getStatus() != Cinema.CinemaStatus.APPROVED) {
            throw new RuntimeException("无权操作该影院或影院未批准");
        }
        return cinema;
    }

    /**
     * 校验影院管理员是否有权限操作指定的影厅
     * @param roomId 影厅 ID
     * @param adminUserId 影院管理员用户 ID
     * @return 影厅对象，如果无权限则抛出异常
     */
    private Room checkRoomPermission(Long roomId, Long adminUserId) {
        Room room = this.getById(roomId);
        if (room == null) {
            throw new RuntimeException("影厅不存在");
        }
        checkCinemaAdminPermission(room.getCinemaId(), adminUserId); // 复用影院权限检查
        return room;
    }

    @Override
    public Room addRoom(Room room, Long adminUserId) {
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);

        if (cinema == null) {
            log.warn("addRoom: User {} has no associated cinema.", adminUserId);
        } else {
            log.info("addRoom: Found cinema for user {}: id={}, status={}", adminUserId, cinema.getId(), cinema.getStatus());
        }

        if (cinema == null || cinema.getStatus() != Cinema.CinemaStatus.APPROVED) {
             throw new RuntimeException("无权操作该影院或影院未批准");
        }
        room.setCinemaId(cinema.getId());

        // 设置默认行列数 (如果前端未提供)
        if (room.getRowsCount() == null) {
            room.setRowsCount(8);
        }
        if (room.getColsCount() == null) {
            room.setColsCount(8);
        }
        boolean saved = this.save(room);
        if (!saved) {
            throw new RuntimeException("添加影厅失败");
        }
        return room;
    }

    @Override
    public boolean updateRoom(Room room, Long adminUserId) {
        Room existingRoom = checkRoomPermission(room.getId(), adminUserId);
        // 确保 cinemaId 不被修改
        room.setCinemaId(existingRoom.getCinemaId());
        return this.updateById(room);
    }

    @Override
    public boolean deleteRoom(Long roomId, Long adminUserId) {
        checkRoomPermission(roomId, adminUserId);
        // TODO: 检查该影厅是否有未完成的场次等关联数据，决定是否允许删除
        return this.removeById(roomId);
    }

    @Override
    public List<Room> listRoomsByCinemaId(Long cinemaId, Long adminUserId) {
        checkCinemaAdminPermission(cinemaId, adminUserId);
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Room::getCinemaId, cinemaId);
        wrapper.orderByAsc(Room::getName); // 按名称排序
        return this.list(wrapper);
    }

    @Override
    public Room getRoomById(Long roomId, Long adminUserId) {
        return checkRoomPermission(roomId, adminUserId);
    }
} 