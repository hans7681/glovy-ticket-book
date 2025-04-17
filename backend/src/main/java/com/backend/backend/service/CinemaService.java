package com.backend.backend.service;

import com.backend.backend.entity.Cinema;
import com.baomidou.mybatisplus.extension.service.IService;

public interface CinemaService extends IService<Cinema> {

    /**
     * 影院管理员申请或注册影院
     * @param cinema 影院信息 (此时 adminUserId 可能为空或为申请人ID)
     * @param applicantUserId 申请人（影院管理员）的用户 ID
     * @return 创建的影院对象
     */
    Cinema applyForCinema(Cinema cinema, Long applicantUserId);

    /**
     * 系统管理员审核影院
     * @param cinemaId 影院ID
     * @param approved 是否批准
     * @param adminUserId 如果批准，关联的影院管理员 User ID (可选，也可后续关联)
     * @return 更新后的影院对象
     */
    Cinema reviewCinema(Long cinemaId, boolean approved, Long adminUserId);

    /**
     * 更新影院状态 (例如禁用/启用)
     * @param cinemaId 影院ID
     * @param status 新的状态
     * @return 是否成功
     */
    boolean updateCinemaStatus(Long cinemaId, Cinema.CinemaStatus status);

    /**
     * 根据影院管理员ID获取其管理的影院信息
     * @param adminUserId 影院管理员用户 ID
     * @return 影院信息，如果不存在或未批准则返回 null
     */
    Cinema getCinemaByAdminUserId(Long adminUserId);

    /**
     * 影院管理员更新自己影院的信息
     * @param cinema 包含要更新信息的影院对象
     * @param adminUserId 操作的影院管理员用户 ID (用于验证权限)
     * @return 是否成功
     */
    boolean updateCinemaByAdmin(Cinema cinema, Long adminUserId);
    
    /**
     * 分页获取已批准的影院列表，支持按名称、地址和特色进行筛选
     * @param current 当前页码
     * @param size 每页大小
     * @param name 影院名称(模糊匹配)，可为null
     * @param location 地址关键词(模糊匹配)，可为null
     * @param feature 特色设施关键词(模糊匹配)，可为null
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<Cinema> listApprovedCinemas(
            int current, int size, String name, String location, String feature);

    /**
     * 处理用户提交的影院合作入驻申请
     * @param applicationRequestDTO 包含申请信息的 DTO
     * @param userId 申请用户的 ID
     * @return 创建的影院对象 (状态为 PENDING_APPROVAL)
     * @throws RuntimeException 如果用户角色不符、已有申请或创建失败
     */
    Cinema handlePartnershipApplication(com.backend.backend.dto.CinemaPartnershipApplicationRequestDTO applicationRequestDTO, Long userId);
} 