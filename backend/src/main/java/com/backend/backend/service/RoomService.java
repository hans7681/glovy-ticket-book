package com.backend.backend.service;

import com.backend.backend.entity.Room;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface RoomService extends IService<Room> {

    /**
     * 影院管理员添加影厅
     * @param room 影厅信息
     * @param adminUserId 操作的影院管理员 ID
     * @return 创建的影厅对象
     */
    Room addRoom(Room room, Long adminUserId);

    /**
     * 影院管理员更新影厅信息
     * @param room 包含更新信息的影厅对象
     * @param adminUserId 操作的影院管理员 ID
     * @return 是否成功
     */
    boolean updateRoom(Room room, Long adminUserId);

    /**
     * 影院管理员删除影厅
     * @param roomId 要删除的影厅 ID
     * @param adminUserId 操作的影院管理员 ID
     * @return 是否成功
     */
    boolean deleteRoom(Long roomId, Long adminUserId);

    /**
     * 获取指定影院的所有影厅列表
     * @param cinemaId 影院 ID
     * @param adminUserId 操作的影院管理员 ID (用于验证权限)
     * @return 影厅列表
     */
    List<Room> listRoomsByCinemaId(Long cinemaId, Long adminUserId);

     /**
      * 获取指定影厅的详情
      * @param roomId 影厅 ID
      * @param adminUserId 操作的影院管理员 ID (用于验证权限)
      * @return 影厅对象
      */
    Room getRoomById(Long roomId, Long adminUserId);
} 