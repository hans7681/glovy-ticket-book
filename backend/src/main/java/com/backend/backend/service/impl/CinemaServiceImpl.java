package com.backend.backend.service.impl;

import com.backend.backend.dto.CinemaPartnershipApplicationRequestDTO;
import com.backend.backend.entity.Cinema;
import com.backend.backend.entity.User; // 假设需要检查用户角色
import com.backend.backend.mapper.CinemaMapper;
import com.backend.backend.service.CinemaService;
import com.backend.backend.service.UserService; // 用于获取用户信息
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinema> implements CinemaService {

    private static final Logger log = LoggerFactory.getLogger(CinemaServiceImpl.class);

    @Autowired
    private CinemaMapper cinemaMapper;

    @Autowired
    private UserService userService; // 用于检查申请人角色和关联管理员

    @Override
    @Transactional
    public Cinema applyForCinema(Cinema cinema, Long applicantUserId) {
        // 1. 验证申请人角色是否为 CINEMA_ADMIN (简化处理，实际应从 SecurityContext 获取)
        User applicant = userService.getById(applicantUserId); // 使用 getById
        if (applicant == null || applicant.getRole() != User.UserRole.CINEMA_ADMIN) {
            throw new RuntimeException("只有影院管理员才能申请影院");
        }

        // 2. 检查该管理员是否已关联其他影院
        Cinema existingCinema = cinemaMapper.findByAdminUserId(applicantUserId);
        if (existingCinema != null) {
            throw new RuntimeException("该管理员已关联影院: " + existingCinema.getName());
        }

        // 3. 设置初始状态和关联的管理员ID
        cinema.setStatus(Cinema.CinemaStatus.PENDING_APPROVAL);
        cinema.setAdminUserId(applicantUserId); // 直接关联申请人
        // cinema.setAdminUserId(null); // 或者先不关联，等管理员审核时再指定

        // 4. 保存影院信息
        boolean saved = this.save(cinema);
        if (!saved) {
            throw new RuntimeException("申请影院失败");
        }
        return cinema;
    }

    @Override
    @Transactional
    public Cinema reviewCinema(Long cinemaId, boolean approved, Long adminUserIdFromRequest) {
        Cinema cinema = this.getById(cinemaId);
        if (cinema == null) {
            throw new RuntimeException("影院不存在");
        }
        if (cinema.getStatus() != Cinema.CinemaStatus.PENDING_APPROVAL) {
            throw new RuntimeException("该影院不处于待审核状态");
        }

        if (approved) {
            cinema.setStatus(Cinema.CinemaStatus.APPROVED);
            // 获取申请时关联的用户ID
            Long associatedUserId = cinema.getAdminUserId();
            
            if (associatedUserId == null) {
                // 这通常不应发生，因为 handlePartnershipApplication 会设置 adminUserId
                log.error("审批影院 ID {} 时发现关联的 adminUserId 为空！", cinemaId);
                throw new RuntimeException("数据错误：待审核的影院未关联申请用户ID");
            }

            // --- 更新用户角色 --- 
            User userToUpdate = userService.getById(associatedUserId);
            if (userToUpdate != null) {
                // 只有当用户当前是 USER 时才更新为 CINEMA_ADMIN
                if (userToUpdate.getRole() == User.UserRole.USER) {
                    log.info("影院 ID {} 审核通过，将用户 ID {} 的角色从 USER 更新为 CINEMA_ADMIN", cinemaId, associatedUserId);
                    userToUpdate.setRole(User.UserRole.CINEMA_ADMIN);
                    boolean userUpdated = userService.updateById(userToUpdate);
                    if (!userUpdated) {
                        // 如果用户角色更新失败，则回滚整个事务
                        log.error("更新用户 ID {} 角色失败，影院审核操作已回滚", associatedUserId);
                        throw new RuntimeException("更新用户角色失败，影院审核操作已回滚");
                    }
                } else if (userToUpdate.getRole() != User.UserRole.CINEMA_ADMIN) {
                     // 如果用户已经是其他管理员角色，记录警告，不进行修改
                     log.warn("影院 ID {} 审核通过，但关联用户 {} 当前角色为 {}，非 USER，角色未自动变更为 CINEMA_ADMIN", 
                              cinemaId, associatedUserId, userToUpdate.getRole());
                } 
                // 如果用户已经是 CINEMA_ADMIN，则无需操作
            } else {
                // 如果关联的用户 ID 在数据库中找不到，这是一个严重问题
                 log.error("影院 ID {} 审核通过，但找不到关联的用户 ID {}，无法更新用户角色！", cinemaId, associatedUserId);
                 throw new RuntimeException("关联的用户ID无效，无法更新用户角色");
            }
            // --- 用户角色更新结束 --- 

        } else {
            // 拒绝申请
            cinema.setStatus(Cinema.CinemaStatus.REJECTED);
            // 拒绝时解绑 adminUserId，允许该用户重新申请或其他操作
            log.info("影院 ID {} 审核被拒绝，解除用户 ID {} 的关联", cinemaId, cinema.getAdminUserId());
            cinema.setAdminUserId(null);
        }

        // 更新影院信息
        boolean cinemaUpdated = this.updateById(cinema);
        if (!cinemaUpdated) {
            // 如果影院状态更新失败，事务也会回滚
            log.error("更新影院 ID {} 状态失败", cinemaId);
            throw new RuntimeException("更新影院状态失败");
        }
        
        log.info("影院 ID {} 审核处理完成，最终状态: {}, 关联用户ID: {}", 
                 cinemaId, cinema.getStatus(), cinema.getAdminUserId());
        return cinema;
    }

    @Override
    public boolean updateCinemaStatus(Long cinemaId, Cinema.CinemaStatus status) {
        Cinema cinema = this.getById(cinemaId);
        if (cinema == null) {
            return false; // 或者抛出异常
        }
        // 只有 APPROVED 和 DISABLED 状态可以互相切换，或从其他状态变为 DISABLED
        if (!isValidStatusTransition(cinema.getStatus(), status)) {
            throw new RuntimeException("无效的状态变更");
        }
        cinema.setStatus(status);
        return this.updateById(cinema);
    }

    private boolean isValidStatusTransition(Cinema.CinemaStatus current, Cinema.CinemaStatus next) {
        if (current == next) return true;
        if (current == Cinema.CinemaStatus.APPROVED && next == Cinema.CinemaStatus.DISABLED) return true;
        if (current == Cinema.CinemaStatus.DISABLED && next == Cinema.CinemaStatus.APPROVED) return true;
        // 其他状态变为 DISABLED 也是允许的
        if (next == Cinema.CinemaStatus.DISABLED) return true;
        // 其他转换不允许
        return false;
    }


    @Override
    public Cinema getCinemaByAdminUserId(Long adminUserId) {
        return cinemaMapper.findByAdminUserId(adminUserId);
    }

    @Override
    public boolean updateCinemaByAdmin(Cinema cinema, Long adminUserId) {
        Cinema existingCinema = this.getById(cinema.getId());
        if (existingCinema == null) {
            throw new RuntimeException("影院不存在");
        }
        // 权限校验：确保操作者是该影院的管理员，并且影院是 APPROVED 状态
        if (!adminUserId.equals(existingCinema.getAdminUserId())) {
            throw new RuntimeException("无权修改该影院信息");
        }
        if (existingCinema.getStatus() != Cinema.CinemaStatus.APPROVED) {
             throw new RuntimeException("只有已批准的影院才能修改信息");
        }

        // 防止关键信息被修改 (如 adminUserId, status)
        cinema.setAdminUserId(existingCinema.getAdminUserId());
        cinema.setStatus(existingCinema.getStatus());
        cinema.setCreateTime(existingCinema.getCreateTime()); // 防止创建时间被修改

        return this.updateById(cinema);
    }
    
    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<Cinema> listApprovedCinemas(
            int current, int size, String name, String location, String feature) {
        
        // 创建查询条件
        LambdaQueryWrapper<Cinema> queryWrapper = new LambdaQueryWrapper<>();
        
        // 默认只查询已批准的影院
        queryWrapper.eq(Cinema::getStatus, Cinema.CinemaStatus.APPROVED);
        
        // 根据提供的条件进行筛选
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like(Cinema::getName, name.trim());
        }
        
        if (location != null && !location.trim().isEmpty()) {
            queryWrapper.like(Cinema::getAddress, location.trim());
        }
        
        if (feature != null && !feature.trim().isEmpty()) {
            queryWrapper.like(Cinema::getDescription, feature.trim());
        }
        
        // 按照创建时间倒序排列，展示最新的影院
        queryWrapper.orderByDesc(Cinema::getCreateTime);
        
        // 执行分页查询
        return this.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size), queryWrapper);
    }

    @Override
    @Transactional
    public Cinema handlePartnershipApplication(com.backend.backend.dto.CinemaPartnershipApplicationRequestDTO applicationRequestDTO, Long userId) {
        // 1. 验证用户角色
        User applicant = userService.getById(userId);
        if (applicant == null || applicant.getRole() != User.UserRole.USER) {
            throw new RuntimeException("只有普通用户才能申请影院合作"); // 或抛出更具体的权限异常
        }

        // 2. 检查用户是否已有影院或待处理申请
        LambdaQueryWrapper<Cinema> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cinema::getAdminUserId, userId)
                    .and(wrapper -> wrapper.eq(Cinema::getStatus, Cinema.CinemaStatus.APPROVED)
                                           .or().eq(Cinema::getStatus, Cinema.CinemaStatus.PENDING_APPROVAL));
        if (this.count(queryWrapper) > 0) {
            throw new RuntimeException("您已有关联的影院或正在审核中的申请"); // 409 Conflict
        }

        // 3. 创建新的 Cinema 实体
        Cinema newCinema = new Cinema();
        newCinema.setName(applicationRequestDTO.getName());
        newCinema.setAddress(applicationRequestDTO.getAddress());
        newCinema.setPhone(applicationRequestDTO.getPhone());
        newCinema.setLogo(applicationRequestDTO.getLogo());
        newCinema.setDescription(applicationRequestDTO.getDescription());

        // 4. 设置状态和关联用户
        newCinema.setStatus(Cinema.CinemaStatus.PENDING_APPROVAL);
        newCinema.setAdminUserId(userId);

        // 5. 保存到数据库
        boolean saved = this.save(newCinema);
        if (!saved) {
            throw new RuntimeException("提交影院申请失败"); // 500 Internal Server Error or specific error
        }

        return newCinema;
    }
} 