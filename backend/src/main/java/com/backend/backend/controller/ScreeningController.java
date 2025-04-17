package com.backend.backend.controller;

import com.backend.backend.dto.LockSeatsRequest;
import com.backend.backend.dto.ScreeningRequest;
import com.backend.backend.dto.ScreeningReviewRequest;
import com.backend.backend.dto.UnlockSeatsRequest;
import com.backend.backend.dto.SeatIdentifier;
import com.backend.backend.entity.Screening;
import com.backend.backend.entity.SeatLock;
import com.backend.backend.entity.Cinema;
import com.backend.backend.entity.Movie;
import com.backend.backend.entity.Room;
import com.backend.backend.exception.SeatUnavailableException;
import com.backend.backend.service.ScreeningService;
import com.backend.backend.service.SeatService;
import com.backend.backend.service.SeatLockService;
import com.backend.backend.service.CinemaService;
import com.backend.backend.service.RoomService;
import com.backend.backend.service.MovieService;
import com.backend.backend.util.SecurityUtil;
import com.backend.backend.vo.ScreeningWithNamesVO;
import com.backend.backend.vo.SeatStatusVO;
import com.backend.backend.vo.ScreeningDetailVO;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Tag(name = "场次管理", description = "包含影院管理员申请/管理、系统管理员审批、用户查询场次的接口")
@Slf4j
@RestController
@RequestMapping("/api")
public class ScreeningController {

    @Autowired
    private ScreeningService screeningService;

    @Autowired
    private SeatService seatService;

    @Autowired
    private SeatLockService seatLockService;

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private MovieService movieService;

    // --- 影院管理员接口 ---
    @Tag(name = "影院管理员 - 场次管理")
    @Operation(
        summary = "申请新场次", 
        description = "影院管理员为其影院的影厅申请新的电影放映场次。申请后场次状态为 PENDING_APPROVAL。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "场次申请创建成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Screening.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"cinemaId\": 1,\n" +
                           "  \"roomId\": 1,\n" +
                           "  \"movieId\": 1,\n" +
                           "  \"startTime\": \"2025-01-30T10:00:00\",\n" +
                           "  \"endTime\": \"2025-01-30T12:00:00\",\n" +
                           "  \"price\": 45.00,\n" +
                           "  \"status\": \"PENDING_APPROVAL\",\n" +
                           "  \"createTime\": \"2024-05-20T14:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如时间冲突或价格无效",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"指定时间段内该影厅已有排片\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限")
    })
    @PostMapping("/cinema-admin/screenings")
    public ResponseEntity<?> applyForScreening(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "场次申请信息，包含影厅ID、电影ID、开始时间和价格。cinemaId 将由后端根据管理员信息自动填充。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ScreeningRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"roomId\": 1,\n" +
                               "  \"movieId\": 1,\n" +
                               "  \"startTime\": \"2025-01-30T10:00:00\",\n" +
                               "  \"price\": 45.00\n" +
                               "}"
                    )
                )
            )
            @RequestBody ScreeningRequest screeningRequest) {
        Long currentUserId = SecurityUtil.getCurrentUserId(); // 替换模拟数据
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        try {
            Screening createdScreening = screeningService.applyForScreening(screeningRequest, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdScreening);
        } catch (RuntimeException e) {
            // 根据 Service 抛出的异常类型细化错误响应
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Tag(name = "影院管理员 - 场次管理")
    @Operation(
        summary = "获取自己影院的场次列表", 
        description = "分页获取当前管理员所属影院的场次列表，可按电影、日期、状态筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取场次列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"cinemaId\": 1,\n" +
                           "      \"roomId\": 1,\n" +
                           "      \"movieId\": 1,\n" +
                           "      \"startTime\": \"2025-01-30T10:00:00\",\n" +
                           "      \"endTime\": \"2025-01-30T12:00:00\",\n" +
                           "      \"price\": 45.00,\n" +
                           "      \"status\": \"APPROVED\"\n" +
                           "    }\n" +
                           "  ],\n" +
                           "  \"total\": 1,\n" +
                           "  \"size\": 10,\n" +
                           "  \"current\": 1,\n" +
                           "  \"pages\": 1\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限")
    })
    @GetMapping("/cinema-admin/screenings")
    public ResponseEntity<?> listMyScreenings(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按电影 ID 筛选", example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY) 
            @RequestParam(required = false) Long movieId,
            
            @Parameter(description = "按日期筛选 (格式: yyyy-MM-dd)", example = "2025-01-30", schema = @Schema(type = "string", format = "date"), in = ParameterIn.QUERY)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(description = "按状态筛选", schema = @Schema(implementation = Screening.ScreeningStatus.class), example = "APPROVED", in = ParameterIn.QUERY)
            @RequestParam(required = false) Screening.ScreeningStatus status) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId(); // 替换模拟数据
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        Page<Screening> page = new Page<>(current, size);
        try {
             Page<Screening> resultPage = screeningService.listScreeningsByCinemaAdmin(page, currentUserId, movieId, date, status);
             return ResponseEntity.ok(resultPage);
         } catch (RuntimeException e) {
             // 处理无权限等情况 (Service 层应抛出特定异常)
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage()); // 或者返回带错误信息的 Body
         }
    }

    @Tag(name = "影院管理员 - 场次管理")
    @Operation(
        summary = "获取场次详情", 
        description = "获取当前管理员所属影院的单个场次详情。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取场次详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Screening.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"cinemaId\": 1,\n" +
                           "  \"roomId\": 1,\n" +
                           "  \"movieId\": 1,\n" +
                           "  \"startTime\": \"2025-01-30T10:00:00\",\n" +
                           "  \"endTime\": \"2025-01-30T12:00:00\",\n" +
                           "  \"price\": 45.00,\n" +
                           "  \"status\": \"APPROVED\",\n" +
                           "  \"createTime\": \"2024-05-20T14:00:00\",\n" +
                           "  \"updateTime\": \"2024-05-21T10:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或无权查看该场次"),
        @ApiResponse(responseCode = "404", description = "场次不存在")
    })
    @GetMapping("/cinema-admin/screenings/{id}")
    public ResponseEntity<?> getMyScreeningById(
            @Parameter(description = "场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId(); // 替换模拟数据
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        try {
            Screening screening = screeningService.getScreeningByIdForCinemaAdmin(id, currentUserId);
            return screening != null ? ResponseEntity.ok(screening) : ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
             // 根据 Service 抛出的异常判断是 403 还是其他错误
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @Tag(name = "影院管理员 - 场次管理")
    @Operation(
        summary = "取消场次", 
        description = "取消待审批(PENDING_APPROVAL)或未开始(APPROVED)的已批准场次。已开始或已结束的场次无法取消。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "场次取消成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "取消失败，例如场次状态不允许取消",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"场次已开始，无法取消\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或无权操作该场次"),
        @ApiResponse(responseCode = "404", description = "场次不存在")
    })
    @DeleteMapping("/cinema-admin/screenings/{id}") // 使用 DELETE 通常表示删除资源，但这里用于取消
    public ResponseEntity<?> cancelScreening(
            @Parameter(description = "要取消的场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long id) {
        Long currentUserId = SecurityUtil.getCurrentUserId(); // 替换模拟数据
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        try {
            boolean success = screeningService.cancelScreening(id, currentUserId);
            return success ? ResponseEntity.noContent().build() : ResponseEntity.badRequest().body("取消失败或状态不允许");
        } catch (RuntimeException e) {
            // 根据 Service 异常判断 403, 404 或 400
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("无权")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            } else {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }
    }

     // TODO: 实现更新场次的接口 (PUT /cinema-admin/screenings/{id})，需要明确业务规则
     // 需要添加完整的 @Operation, @ApiResponses, @Parameter 等注解

    // --- 系统管理员接口 ---
    @Tag(name = "系统管理员 - 场次管理")
    @Operation(
        summary = "获取场次列表 (分页)", 
        description = "系统管理员获取所有场次列表，可按影院、电影、日期、状态筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "成功获取场次列表 (包含影院、影厅、电影名称)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/admin/screenings")
    public ResponseEntity<Page<ScreeningDetailVO>> listScreeningsForAdmin(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按影院 ID 筛选", example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY) 
            @RequestParam(required = false) Long cinemaId,
            
            @Parameter(description = "按电影 ID 筛选", example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY) 
            @RequestParam(required = false) Long movieId,
            
            @Parameter(description = "按日期筛选 (格式: yyyy-MM-dd)", example = "2025-01-30", schema = @Schema(type = "string", format = "date"), in = ParameterIn.QUERY)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            
            @Parameter(description = "按状态筛选", schema = @Schema(implementation = Screening.ScreeningStatus.class), example = "PENDING_APPROVAL", in = ParameterIn.QUERY)
            @RequestParam(required = false) Screening.ScreeningStatus status) {
        // 此接口需要系统管理员权限，SecurityConfig 会处理
        Page<Screening> page = new Page<>(current, size);
        Page<Screening> screeningPage = screeningService.listScreeningsBySystemAdmin(page, cinemaId, movieId, date, status);
        
        // Convert Page<Screening> to Page<ScreeningDetailVO>
        IPage<ScreeningDetailVO> screeningVoPage = screeningPage.convert(screening -> {
            ScreeningDetailVO vo = new ScreeningDetailVO();
            vo.setScreeningId(screening.getId());
            vo.setMovieId(screening.getMovieId());
            vo.setCinemaId(screening.getCinemaId());
            vo.setRoomId(screening.getRoomId());
            vo.setStartTime(screening.getStartTime());
            vo.setEndTime(screening.getEndTime());
            vo.setPrice(screening.getPrice());
            // Status might be useful too
            vo.setStatus(screening.getStatus());

            // Fetch related names
            Cinema cinema = cinemaService.getById(screening.getCinemaId());
            if (cinema != null) {
                vo.setCinemaName(cinema.getName());
            }
            Room room = roomService.getById(screening.getRoomId());
            if (room != null) {
                vo.setRoomName(room.getName());
                vo.setRoomRowsCount(room.getRowsCount()); // Populate optional fields
                vo.setRoomColsCount(room.getColsCount());
            }
            Movie movie = movieService.getById(screening.getMovieId());
            if (movie != null) {
                vo.setMovieTitle(movie.getTitle());
                vo.setMoviePosterUrl(movie.getPosterUrl()); // Populate optional fields
                vo.setMovieDuration(movie.getDuration());
            }
            
            return vo;
        });

        // Create the final Page object for the response
        Page<ScreeningDetailVO> finalPage = new Page<>(screeningVoPage.getCurrent(), screeningVoPage.getSize(), screeningVoPage.getTotal());
        finalPage.setRecords(screeningVoPage.getRecords());
        finalPage.setPages(screeningVoPage.getPages());

        return ResponseEntity.ok(finalPage);
    }

    @Tag(name = "系统管理员 - 场次管理")
    @Operation(
        summary = "审批场次申请", 
        description = "审批处于 PENDING_APPROVAL 状态的场次申请。批准后状态变为 APPROVED，拒绝则变为 REJECTED。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "审批成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Screening.class) // 返回更新后的场次信息
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "审批失败，例如场次状态不是待审批",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"该场次不是待审批状态\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "场次不存在")
    })
    @PutMapping("/admin/screenings/{id}/review")
    public ResponseEntity<?> reviewScreening(
            @Parameter(description = "待审批的场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "审批请求，包含是否批准",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ScreeningReviewRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"approved\": true\n" +
                               "}"
                    )
                )
            )
            @RequestBody ScreeningReviewRequest reviewRequest) {
         try {
            Screening reviewedScreening = screeningService.reviewScreening(id, reviewRequest.isApproved());
            return ResponseEntity.ok(reviewedScreening);
        } catch (RuntimeException e) {
            // 根据 Service 异常判断 404 或 400
            if (e.getMessage().contains("不存在")) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }
    }

    // --- 普通用户接口 ---
    @Tag(name = "用户 - 场次查询")
    @Operation(
        summary = "查询可用场次列表", 
        description = "根据电影ID和日期查询已批准(APPROVED)且未结束的场次列表，包含影院和影厅名称。可选择指定影院进行筛选。此接口不需要登录。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取可用场次列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class) // Swagger 可能需要额外配置才能正确显示泛型 VO
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如日期格式不正确",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"日期参数不能为空\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        )
    })
    @GetMapping("/screenings")
    public ResponseEntity<Page<ScreeningWithNamesVO>> listAvailableScreenings(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "电影 ID (可选)", required = false, example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY)
            @RequestParam(required = false) Long movieId,
            
            @Parameter(description = "影院 ID (可选)，不传则查询所有影院", example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY)
            @RequestParam(required = false) Long cinemaId,
            
            @Parameter(description = "查询日期 (格式: yyyy-MM-dd)", required = true, example = "2025-01-30", schema = @Schema(type = "string", format = "date"), in = ParameterIn.QUERY)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
         // 创建 Page 对象用于请求
         Page<Screening> pageRequest = new Page<>(current, size);
         try {
             // 调用 Service 方法，该方法返回 Page<ScreeningWithNamesVO>
             Page<ScreeningWithNamesVO> resultPage = screeningService.listAvailableScreeningsForUser(pageRequest, movieId, cinemaId, date);
             return ResponseEntity.ok(resultPage);
         } catch (IllegalArgumentException e) {
             // 对于参数错误，直接返回 400 Bad Request
             // 注意：需要确保 Service 层真的只在参数错误时抛出 IllegalArgumentException
             // 为了类型安全，可以考虑返回 ResponseEntity<?> 并构建错误响应体
             // 这里简化处理，直接返回 Body 为错误消息字符串
             // return ResponseEntity.badRequest().body(e.getMessage());
             // 或者更标准的返回方式:
              return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST); // 或者带错误消息的 Body
         }
    }

    @Tag(name = "用户 - 场次查询")
    @Operation(
            summary = "获取单个场次详情",
            description = "获取指定场次的详细信息，用于用户选座页面展示。此接口通常不需要登录，除非业务要求。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功获取场次详情",
                         content = @Content(mediaType = "application/json",
                                         schema = @Schema(implementation = ScreeningDetailVO.class))),
            @ApiResponse(responseCode = "404", description = "场次不存在", content = @Content)
            // Add 400/500 if needed based on service layer exceptions
    })
    @GetMapping("/screenings/{screeningId}")
    public ResponseEntity<?> getScreeningDetails(
            @Parameter(description = "场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long screeningId) {
        try {
            ScreeningDetailVO screeningDetails = screeningService.getScreeningDetails(screeningId);
            return ResponseEntity.ok(screeningDetails);
        } catch (ScreeningNotFoundException e) {
            log.warn("Screening details requested for non-existent ID: {}", screeningId);
            // Return standard 404 without exposing internal messages typically
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", e.getMessage())); 
        } catch (RuntimeException e) {
            // Catch potential runtime exceptions from data fetching (e.g., related entity missing)
            log.error("Error fetching screening details for ID {}: {}", screeningId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "获取场次详情时发生内部错误"));
        }
    }

    // --- 座位相关接口 ---
    @Tag(name = "用户 - 在线选座")
    @Operation(
            summary = "获取场次座位状态",
            description = "获取指定场次的座位布局以及每个座位的当前状态（可用、已售、已锁定）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功获取座位图",
                         content = @Content(mediaType = "application/json",
                                         schema = @Schema(type = "array", implementation = List.class)
                                        )
                        ),
            @ApiResponse(responseCode = "404", description = "场次不存在或影厅信息无效", content = @Content),
            @ApiResponse(responseCode = "400", description = "场次状态不允许查看座位图", content = @Content)
    })
    @GetMapping("/screenings/{screeningId}/seats")
    public ResponseEntity<List<List<SeatStatusVO>>> getScreeningSeatStatus(
            @Parameter(description = "场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long screeningId) {
        try {
            List<List<SeatStatusVO>> seatMap = seatService.getSeatStatus(screeningId);
            return ResponseEntity.ok(seatMap);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在") || e.getMessage().contains("无效")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            } else if (e.getMessage().contains("不允许")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            } else {
                log.error("Error getting seat status for screening {}: {}", screeningId, e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @Tag(name = "用户 - 在线选座")
    @Operation(
            summary = "锁定座位",
            description = "尝试为当前用户锁定指定场次的多个座位。成功后返回锁定的记录，失败则返回错误信息。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "座位锁定成功",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(type = "array", implementation = SeatLock.class))),
            @ApiResponse(responseCode = "400", description = "请求无效（如未选择座位、重复座位、无效座位号）", content = @Content),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content),
            @ApiResponse(responseCode = "404", description = "场次不存在", content = @Content),
            @ApiResponse(responseCode = "409", description = "座位已被锁定或售出", content = @Content)
    })
    @PostMapping("/screenings/{screeningId}/lock-seats")
    public ResponseEntity<?> lockSeats(
            @Parameter(description = "场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long screeningId,
            @RequestBody LockSeatsRequest request) {

        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "用户未登录"));
        }

        if (request == null || request.getSeatsToLock() == null || request.getSeatsToLock().isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "未选择任何座位"));
        }

        int lockDurationSeconds = 600;

        try {
            List<SeatLock> lockedSeats = seatLockService.lockSeats(screeningId, userId, request.getSeatsToLock(), lockDurationSeconds);
            return ResponseEntity.ok(lockedSeats);
        } catch (SeatUnavailableException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", e.getMessage()));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("场次不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("message", e.getMessage()));
            }
            log.error("Error during seat locking for screening {} user {}: {}", screeningId, userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", "锁定座位时发生内部错误"));
        }
    }

    @Tag(name = "用户 - 在线选座")
    @Operation(
            summary = "解锁座位",
            description = "尝试为当前用户解锁先前锁定的指定场次的座位。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "座位解锁成功", content = @Content),
            @ApiResponse(responseCode = "400", description = "请求无效（如未提供座位列表）", content = @Content),
            @ApiResponse(responseCode = "401", description = "用户未登录", content = @Content),
            @ApiResponse(responseCode = "404", description = "场次不存在或未找到要解锁的座位记录 (对于该用户)", content = @Content) // 404 可能更合适，因为是尝试删除不存在的资源
    })
    @DeleteMapping("/screenings/{screeningId}/unlock-seats")
    public ResponseEntity<?> unlockSeats(
            @Parameter(description = "场次 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64"))
            @PathVariable Long screeningId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "需要解锁的座位列表",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UnlockSeatsRequest.class),
                            examples = @ExampleObject(
                                    value = "{\n" +
                                           "  \"seatsToUnlock\": [\n" +
                                           "    {\"rowIndex\": 5, \"colIndex\": 6},\n" +
                                           "    {\"rowIndex\": 5, \"colIndex\": 7}\n" +
                                           "  ]\n" +
                                           "}"
                            )
                    )
            )
            @RequestBody UnlockSeatsRequest request) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            log.warn("Attempt to unlock seats without authentication for screening {}", screeningId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录，无法解锁座位");
        }

        if (request == null || request.getSeatsToUnlock() == null || request.getSeatsToUnlock().isEmpty()) {
            return ResponseEntity.badRequest().body("请求体无效或未指定需要解锁的座位");
        }

        try {
            boolean success = seatLockService.unlockSeats(screeningId, currentUserId, request.getSeatsToUnlock());
            if (success) {
                log.info("User {} successfully unlocked seats for screening {}", currentUserId, screeningId);
                return ResponseEntity.noContent().build(); // 204 No Content is standard for successful DELETE
            } else {
                // 如果 service 返回 false，可能表示没有找到匹配的锁定记录（可能已经被解锁或过期）
                log.warn("User {} failed to unlock seats for screening {}. Seats might not exist or belong to another user.", currentUserId, screeningId);
                // 返回 404 Not Found 可能比 400 更合适，因为它表示尝试操作的资源（用户的座位锁）未找到
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("未找到需要解锁的座位记录，或这些座位不属于当前用户"); 
            }
        } catch (Exception e) {
            // 捕获 SeatLockService 可能抛出的其他未预料异常
            log.error("Error unlocking seats for screening {} user {}: {}", screeningId, currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("解锁座位时发生内部错误");
        }
    }

    // TODO: 添加自动清理过期锁的机制 (可能通过 @Scheduled 实现)
} 