package com.backend.backend.service;

import com.backend.backend.entity.SeatLock;
import com.backend.backend.dto.SeatIdentifier;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.backend.backend.exception.SeatsUnavailableException;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatLockService extends IService<SeatLock> {

    /**
     * 查询指定场次当前有效的座位锁定信息
     * @param screeningId 场次ID
     * @return 有效的座位锁定列表
     */
    List<SeatLock> findActiveLocksByScreeningId(Long screeningId);

    /**
     * 尝试为用户锁定指定场次的多个座位
     * @param screeningId 场次ID
     * @param userId 用户ID
     * @param seatsToLock 座位列表 (使用 dto.SeatIdentifier)
     * @param lockDurationSeconds 锁定持续时间（秒）
     * @return 成功锁定的座位列表
     * @throws SeatUnavailableException 如果座位已被锁定或售出
     * @throws IllegalArgumentException 如果请求包含重复座位或无效座位号
     * @throws ScreeningNotFoundException 如果场次不存在
     * @throws RuntimeException 其他运行时错误 (如数据库操作失败)
     */
    List<SeatLock> lockSeats(Long screeningId, Long userId, List<SeatIdentifier> seatsToLock, int lockDurationSeconds);

    /**
     * 为用户解锁指定场次的座位
     * @param screeningId 场次ID
     * @param userId 用户ID
     * @param seatsToUnlock 要解锁的座位列表 (使用 dto.SeatIdentifier)
     * @return 是否成功解锁了至少一个座位 (true 表示成功, false 表示未找到匹配的锁定记录)
     */
    boolean unlockSeats(Long screeningId, Long userId, List<SeatIdentifier> seatsToUnlock);

    /**
     * 清理指定时间点之前过期的座位锁定记录
     * @param expiryThreshold 过期时间阈值
     * @return 删除的过期锁定记录数量
     */
    int deleteExpiredLocks(LocalDateTime expiryThreshold);

    /**
     * 查询指定用户在特定场次中针对指定座位列表的有效锁定记录。
     * 用于在创建订单前精确验证用户是否持有这些座位的有效锁。
     *
     * @param userId        用户 ID
     * @param screeningId   场次 ID
     * @param seatsToVerify 需要验证的座位列表
     * @return 属于该用户、该场次且未过期的、匹配指定座位的锁定记录列表。
     *         如果用户没有锁定所有指定的座位，返回的列表大小会小于 seatsToVerify 的大小。
     */
    List<SeatLock> findActiveUserLocksForSeats(Long userId, Long screeningId, List<SeatIdentifier> seatsToVerify);
} 