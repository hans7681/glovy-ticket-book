package com.backend.backend.service;

import com.backend.backend.dto.ScreeningRequest; // 需要创建
import com.backend.backend.entity.Screening;
import com.backend.backend.vo.ScreeningDetailVO; // Import the new VO
import com.backend.backend.vo.ScreeningWithNamesVO; // 导入 VO
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface ScreeningService extends IService<Screening> {

    /**
     * 影院管理员申请新场次
     * @param screeningRequest 场次申请信息 (DTO)
     * @param adminUserId 操作的影院管理员 ID
     * @return 创建的场次对象
     * @throws RuntimeException 如果时间冲突、信息无效或无权限
     */
    Screening applyForScreening(ScreeningRequest screeningRequest, Long adminUserId);

    /**
     * 系统管理员审核场次
     * @param screeningId 场次ID
     * @param approved 是否批准
     * @return 更新后的场次对象
     * @throws RuntimeException 如果场次不存在或状态不符
     */
    Screening reviewScreening(Long screeningId, boolean approved);

    /**
     * 影院管理员获取自己影院的场次列表 (分页)
     * @param page 分页参数
     * @param adminUserId 影院管理员 ID
     * @param movieId 可选筛选条件：电影 ID
     * @param date 可选筛选条件：日期
     * @param status 可选筛选条件：状态
     * @return
     */
    Page<Screening> listScreeningsByCinemaAdmin(Page<Screening> page, Long adminUserId, Long movieId, LocalDate date, Screening.ScreeningStatus status);

    /**
     * 影院管理员获取场次详情
     * @param screeningId 场次 ID
     * @param adminUserId 影院管理员 ID
     * @return
     */
    Screening getScreeningByIdForCinemaAdmin(Long screeningId, Long adminUserId);

    /**
     * 影院管理员取消待审批或已批准的场次
     * @param screeningId 场次 ID
     * @param adminUserId 影院管理员 ID
     * @return 是否成功
     */
    boolean cancelScreening(Long screeningId, Long adminUserId);

    /**
     * 系统管理员获取场次列表 (分页)
     * @param page 分页参数
     * @param cinemaId 可选筛选条件：影院 ID
     * @param movieId 可选筛选条件：电影 ID
     * @param date 可选筛选条件：日期
     * @param status 可选筛选条件：状态
     * @return
     */
    Page<Screening> listScreeningsBySystemAdmin(Page<Screening> page, Long cinemaId, Long movieId, LocalDate date, Screening.ScreeningStatus status);

    /**
     * 用户获取可用场次列表 (分页)，包含影院和影厅名称
     * @param page 分页参数
     * @param movieId 可选筛选条件：电影 ID
     * @param cinemaId 可选筛选条件：影院 ID
     * @param date 必选筛选条件：日期 (只查指定日期的场次)
     * @return 包含名称的场次分页结果
     */
    Page<ScreeningWithNamesVO> listAvailableScreeningsForUser(Page<Screening> page, Long movieId, Long cinemaId, LocalDate date);

    /**
     * 获取单个场次的详细信息，供用户选座页面使用
     * @param screeningId 场次ID
     * @return 包含电影、影院、影厅详细信息的 VO 对象
     * @throws com.backend.backend.exception.ScreeningNotFoundException 如果场次不存在或状态不允许用户查看
     */
    ScreeningDetailVO getScreeningDetails(Long screeningId);

    /**
     * 根据电影ID查找未来最早的有效排期日期
     * 有效排期定义为: 状态为 APPROVED 且 start_time 在当前时间之后
     *
     * @param movieId 电影ID
     * @return 最早排期日期的字符串 (YYYY-MM-DD)，如果找不到则返回 null
     */
    String findFirstAvailableScreeningDate(Long movieId);

     // TODO: 可能还需要更新场次的方法 (根据业务规则)
     // boolean updateScreening(Screening screening, Long adminUserId);
} 