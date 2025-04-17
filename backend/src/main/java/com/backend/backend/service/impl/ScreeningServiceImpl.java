package com.backend.backend.service.impl;

import com.backend.backend.dto.ScreeningRequest;
import com.backend.backend.entity.*;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.backend.backend.mapper.ScreeningMapper;
import com.backend.backend.service.*;
import com.backend.backend.vo.ScreeningDetailVO;
import com.backend.backend.vo.ScreeningWithNamesVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScreeningServiceImpl extends ServiceImpl<ScreeningMapper, Screening> implements ScreeningService {

    @Autowired
    private ScreeningMapper screeningMapper;

    @Autowired
    private MovieService movieService;

    @Autowired
    private RoomService roomService; // 用于校验影厅和影院权限，也用于获取 Room 名称

    @Autowired
    private CinemaService cinemaService;

    // 添加日期格式化器常量
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    @Override
    @Transactional
    public Screening applyForScreening(ScreeningRequest screeningRequest, Long adminUserId) {
        // 1. 校验影厅是否存在以及管理员是否有权限操作该影厅对应的影院
        Room room = roomService.getRoomById(screeningRequest.getRoomId(), adminUserId); // RoomService 内部会校验权限和影院状态
        Long cinemaId = room.getCinemaId();

        // 2. 校验电影是否存在且状态允许排片 (例如 NOW_PLAYING)
        Movie movie = movieService.getById(screeningRequest.getMovieId());
        if (movie == null || movie.getDuration() == null || movie.getDuration() <= 0) {
            throw new RuntimeException("电影不存在或时长信息无效");
        }
        // if (movie.getStatus() != Movie.MovieStatus.NOW_PLAYING) {
        //     throw new RuntimeException("电影当前状态不允许排片");
        // }

        // 3. 计算结束时间
        LocalDateTime startTime = screeningRequest.getStartTime();
        if (startTime == null || startTime.isBefore(LocalDateTime.now())) {
            throw new RuntimeException("开始时间不能为空或早于当前时间");
        }
        LocalDateTime endTime = startTime.plusMinutes(movie.getDuration());

        // 4. 检查时间冲突
        List<Screening> conflicts = screeningMapper.findConflictingScreenings(
                room.getId(), startTime, endTime, null // 新增时 excludeScreeningId 为 null
        );
        if (!conflicts.isEmpty()) {
            Screening conflict = conflicts.get(0);
            throw new RuntimeException("时间冲突：与场次 " + conflict.getId() + " (" +
                                       conflict.getStartTime() + " - " + conflict.getEndTime() + ") 冲突");
        }

        // 5. 创建 Screening 对象并保存
        Screening screening = new Screening();
        screening.setMovieId(screeningRequest.getMovieId());
        screening.setRoomId(room.getId());
        screening.setCinemaId(cinemaId); // 设置冗余字段
        screening.setStartTime(startTime);
        screening.setEndTime(endTime);
        screening.setPrice(screeningRequest.getPrice());
        screening.setStatus(Screening.ScreeningStatus.PENDING_APPROVAL); // 初始状态

        boolean saved = this.save(screening);
        if (!saved) {
            throw new RuntimeException("申请场次失败");
        }
        return screening;
    }

    @Override
    @Transactional
    public Screening reviewScreening(Long screeningId, boolean approved) {
        Screening screening = this.getById(screeningId);
        if (screening == null) {
            throw new RuntimeException("场次不存在");
        }
        if (screening.getStatus() != Screening.ScreeningStatus.PENDING_APPROVAL) {
            throw new RuntimeException("该场次不处于待审批状态");
        }

        if (approved) {
            // 如果批准，再次检查时间冲突（以防在审核期间有其他场次被批准）
            List<Screening> conflicts = screeningMapper.findConflictingScreenings(
                    screening.getRoomId(), screening.getStartTime(), screening.getEndTime(), screening.getId()
            );
             if (!conflicts.isEmpty()) {
                Screening conflict = conflicts.get(0);
                // 可以选择拒绝，或者让管理员解决冲突
                throw new RuntimeException("审核失败：时间冲突，与已批准场次 " + conflict.getId() + " 冲突");
             }
            screening.setStatus(Screening.ScreeningStatus.APPROVED);
        } else {
            screening.setStatus(Screening.ScreeningStatus.REJECTED);
        }

        boolean updated = this.updateById(screening);
        if (!updated) {
            throw new RuntimeException("审核场次失败");
        }
        return screening;
    }

    @Override
    public Page<Screening> listScreeningsByCinemaAdmin(Page<Screening> page, Long adminUserId, Long movieId, LocalDate date, Screening.ScreeningStatus status) {
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);
        if (cinema == null) {
            // 或者返回空分页
             throw new RuntimeException("当前用户未关联影院");
        }
        LambdaQueryWrapper<Screening> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Screening::getCinemaId, cinema.getId()); // 关键：只查自己影院的
        if (movieId != null) {
            wrapper.eq(Screening::getMovieId, movieId);
        }
        if (date != null) {
            wrapper.ge(Screening::getStartTime, date.atStartOfDay());
            wrapper.lt(Screening::getStartTime, date.plusDays(1).atStartOfDay());
        }
        if (status != null) {
            wrapper.eq(Screening::getStatus, status);
        }
        wrapper.orderByDesc(Screening::getStartTime);
        return screeningMapper.selectPage(page, wrapper);
    }

    @Override
    public Screening getScreeningByIdForCinemaAdmin(Long screeningId, Long adminUserId) {
        Screening screening = this.getById(screeningId);
        if (screening == null) {
            return null;
        }
        // 校验影院权限
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);
        if (cinema == null || !cinema.getId().equals(screening.getCinemaId())) {
            throw new RuntimeException("无权查看该场次信息");
        }
        return screening;
    }

    @Override
    @Transactional
    public boolean cancelScreening(Long screeningId, Long adminUserId) {
        Screening screening = this.getById(screeningId);
        if (screening == null) {
            throw new RuntimeException("场次不存在");
        }
        // 校验影院权限
        Cinema cinema = cinemaService.getCinemaByAdminUserId(adminUserId);
        if (cinema == null || !cinema.getId().equals(screening.getCinemaId())) {
            throw new RuntimeException("无权取消该场次");
        }
        // 只有待审批或已批准且未开始的场次可以取消 (业务规则)
        if (screening.getStatus() == Screening.ScreeningStatus.PENDING_APPROVAL ||
            (screening.getStatus() == Screening.ScreeningStatus.APPROVED && screening.getStartTime().isAfter(LocalDateTime.now()))) {
            // TODO: 检查是否有订单关联，如果有，需要处理退款或不允许取消
            screening.setStatus(Screening.ScreeningStatus.CANCELLED);
            return this.updateById(screening);
        } else {
            throw new RuntimeException("该场次状态不允许取消");
        }
    }

    @Override
    public Page<Screening> listScreeningsBySystemAdmin(Page<Screening> page, Long cinemaId, Long movieId, LocalDate date, Screening.ScreeningStatus status) {
        LambdaQueryWrapper<Screening> wrapper = new LambdaQueryWrapper<>();
        if (cinemaId != null) {
            wrapper.eq(Screening::getCinemaId, cinemaId);
        }
         if (movieId != null) {
            wrapper.eq(Screening::getMovieId, movieId);
        }
        if (date != null) {
            wrapper.ge(Screening::getStartTime, date.atStartOfDay());
            wrapper.lt(Screening::getStartTime, date.plusDays(1).atStartOfDay());
        }
        if (status != null) {
            wrapper.eq(Screening::getStatus, status);
        } else {
            // 系统管理员默认可能想看 PENDING_APPROVAL 的
            // wrapper.eq(Screening::getStatus, Screening.ScreeningStatus.PENDING_APPROVAL);
        }
        wrapper.orderByDesc(Screening::getCreateTime);
        return screeningMapper.selectPage(page, wrapper);
    }

    @Override
    public Page<ScreeningWithNamesVO> listAvailableScreeningsForUser(Page<Screening> pageRequest, Long movieId, Long cinemaId, LocalDate date) {
        // 新检查: date 必须提供，movieId 和 cinemaId 至少提供一个
        if (date == null) {
            throw new IllegalArgumentException("日期不能为空");
        }
        if (movieId == null && cinemaId == null) {
             throw new IllegalArgumentException("电影ID或影院ID必须至少提供一个");
        }

        // 1. 先按条件查询基础的 Screening 分页数据
        LambdaQueryWrapper<Screening> wrapper = new LambdaQueryWrapper<>();

        // 条件化 movieId 查询
        if (movieId != null) {
            wrapper.eq(Screening::getMovieId, movieId);
        }
        // cinemaId 查询保持不变 (已经是条件化的)
        if (cinemaId != null) {
            wrapper.eq(Screening::getCinemaId, cinemaId);
        }
        // 其他查询条件
        wrapper.ge(Screening::getStartTime, date.atStartOfDay());
        wrapper.lt(Screening::getStartTime, date.plusDays(1).atStartOfDay());
        wrapper.eq(Screening::getStatus, Screening.ScreeningStatus.APPROVED);
        wrapper.gt(Screening::getEndTime, LocalDateTime.now()); // 只查询未结束的
        wrapper.orderByAsc(Screening::getStartTime);

        Page<Screening> screeningPage = screeningMapper.selectPage(pageRequest, wrapper);

        // 2. 创建一个新的 Page 对象用于存放 VO 结果，复制分页信息
        Page<ScreeningWithNamesVO> resultPage = new Page<>(screeningPage.getCurrent(), screeningPage.getSize(), screeningPage.getTotal());
        resultPage.setPages(screeningPage.getPages());

        // 3. 如果没有查询到数据，直接返回空 VO 分页
        if (screeningPage.getRecords().isEmpty()) {
            return resultPage;
        }

        // 4. 提取所有需要查询名称的 cinemaId 和 roomId
        List<Long> cinemaIds = screeningPage.getRecords().stream().map(Screening::getCinemaId).distinct().collect(Collectors.toList());
        List<Long> roomIds = screeningPage.getRecords().stream().map(Screening::getRoomId).distinct().collect(Collectors.toList());

        // 5. 批量查询影院和影厅名称
        Map<Long, String> cinemaNameMap = cinemaService.listByIds(cinemaIds).stream()
                .collect(Collectors.toMap(Cinema::getId, Cinema::getName));
        Map<Long, String> roomNameMap = roomService.listByIds(roomIds).stream()
                 .collect(Collectors.toMap(Room::getId, Room::getName));

        // 6. 转换结果为 VO
        List<ScreeningWithNamesVO> voList = screeningPage.getRecords().stream().map(screening -> {
            ScreeningWithNamesVO vo = new ScreeningWithNamesVO();
            BeanUtils.copyProperties(screening, vo);
            vo.setCinemaName(cinemaNameMap.getOrDefault(screening.getCinemaId(), "未知影院"));
            vo.setRoomName(roomNameMap.getOrDefault(screening.getRoomId(), "未知影厅"));
            return vo;
        }).collect(Collectors.toList());

        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    @Transactional(readOnly = true) // 只读事务
    public ScreeningDetailVO getScreeningDetails(Long screeningId) {
        // 1. 查询场次基本信息
        Screening screening = this.getById(screeningId);
        if (screening == null) {
            throw new ScreeningNotFoundException("场次不存在: " + screeningId);
        }
        // 可以在这里添加状态检查，例如只允许查询 APPROVED 的场次
        // if (screening.getStatus() != Screening.ScreeningStatus.APPROVED) {
        //     throw new ScreeningNotFoundException("场次状态不允许查看详情: " + screening.getStatus());
        // }

        // 2. 查询关联信息 (电影、影院、影厅)
        // 使用 Optional 处理可能为 null 的情况，尽管外键约束下通常不为 null，但更健壮
        Movie movie = Optional.ofNullable(movieService.getById(screening.getMovieId()))
                .orElseThrow(() -> new RuntimeException("关联电影数据丢失: movieId=" + screening.getMovieId()));
        Cinema cinema = Optional.ofNullable(cinemaService.getById(screening.getCinemaId()))
                .orElseThrow(() -> new RuntimeException("关联影院数据丢失: cinemaId=" + screening.getCinemaId()));
        Room room = Optional.ofNullable(roomService.getById(screening.getRoomId()))
                .orElseThrow(() -> new RuntimeException("关联影厅数据丢失: roomId=" + screening.getRoomId()));

        // 3. 组装 VO
        ScreeningDetailVO vo = new ScreeningDetailVO();
        vo.setScreeningId(screening.getId());
        vo.setStartTime(screening.getStartTime());
        vo.setEndTime(screening.getEndTime());
        vo.setPrice(screening.getPrice());

        vo.setMovieId(movie.getId());
        vo.setMovieTitle(movie.getTitle());
        vo.setMoviePosterUrl(movie.getPosterUrl()); // 假设 Movie 实体有 posterUrl 字段
        vo.setMovieDuration(movie.getDuration());   // 假设 Movie 实体有 duration 字段

        vo.setCinemaId(cinema.getId());
        vo.setCinemaName(cinema.getName());

        vo.setRoomId(room.getId());
        vo.setRoomName(room.getName());
        vo.setRoomRowsCount(room.getRowsCount()); // 假设 Room 实体有 rowsCount 字段
        vo.setRoomColsCount(room.getColsCount()); // 假设 Room 实体有 colsCount 字段

        return vo;
    }

    @Override
    public String findFirstAvailableScreeningDate(Long movieId) {
        // 调用 Mapper 查询最早的开始时间
        LocalDateTime earliestTime = screeningMapper.findEarliestApprovedScreeningTime(movieId);

        if (earliestTime != null) {
            // 如果找到了时间，将其格式化为 YYYY-MM-DD 字符串
            return earliestTime.format(DATE_FORMATTER);
        } else {
            // 如果未找到，返回 null
            return null;
        }
    }
} 