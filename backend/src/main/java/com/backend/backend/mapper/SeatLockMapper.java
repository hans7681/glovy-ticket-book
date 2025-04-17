package com.backend.backend.mapper;

import com.backend.backend.dto.SeatIdentifier;
import com.backend.backend.entity.SeatLock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SeatLockMapper extends BaseMapper<SeatLock> {

    /**
     * Finds active seat locks for a specific user, screening, and list of seats.
     * The corresponding XML query should filter by userId, screeningId, 
     * check if (row_index, col_index) is in the provided list, 
     * and ensure lock_expiry_time > now.
     *
     * @param userId User ID.
     * @param screeningId Screening ID.
     * @param seats List of SeatIdentifier objects.
     * @param now Current time to check against expiry.
     * @return List of matching active SeatLock entities.
     */
    List<SeatLock> findActiveUserLocksForSeats(
            @Param("userId") Long userId,
            @Param("screeningId") Long screeningId,
            @Param("seats") List<SeatIdentifier> seats,
            @Param("now") LocalDateTime now);

    /**
     * Deletes seat locks matching the user, screening, and specific seats.
     * The corresponding XML should construct a DELETE statement with a WHERE clause covering these conditions.
     *
     * @param userId User ID.
     * @param screeningId Screening ID.
     * @param seats List of SeatIdentifier objects to specify which locks to delete.
     * @return Number of rows affected.
     */
    int deleteUserLocksForSeats(
            @Param("userId") Long userId,
            @Param("screeningId") Long screeningId,
            @Param("seats") List<SeatIdentifier> seats);

    /**
     * Deletes all seat locks where the expiry time is before the specified time.
     *
     * @param expiryTime The time threshold. Locks older than this will be deleted.
     * @return Number of rows affected.
     */
    int deleteExpiredLocks(@Param("expiryTime") LocalDateTime expiryTime);

    /**
     * Checks if specific seats for a screening are currently locked (by anyone).
     * Used during the initial locking phase to prevent locking already locked seats.
     * @param screeningId Screening ID.
     * @param seats List of SeatIdentifier objects.
     * @param now Current time to check against expiry.
     * @return List of SeatLock entities representing currently locked seats from the input list.
     */
    List<SeatLock> findAnyActiveLocksForSeats(
        @Param("screeningId") Long screeningId,
        @Param("seats") List<SeatIdentifier> seats,
        @Param("now") LocalDateTime now);

    /**
     * Finds seats from the given list that are already present in the order_seat table for the screening.
     * Used as an additional check during locking to prevent locking already sold seats.
     * Assumes existence of an OrderSeatMapper or direct query capability.
     * 
     * @param screeningId Screening ID.
     * @param seats List of SeatIdentifier objects.
     * @return List of SeatIdentifier objects that correspond to already sold seats.
     */
    List<SeatIdentifier> findSoldSeatsForScreening(
        @Param("screeningId") Long screeningId,
        @Param("seats") List<SeatIdentifier> seats);

} 