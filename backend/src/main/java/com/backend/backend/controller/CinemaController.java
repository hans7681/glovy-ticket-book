package com.backend.backend.controller;

import com.backend.backend.dto.CinemaPartnershipApplicationRequestDTO;
import com.backend.backend.dto.CinemaReviewRequest;
import com.backend.backend.dto.CinemaStatusUpdateRequest;
import com.backend.backend.entity.Cinema;
import com.backend.backend.entity.User;
import com.backend.backend.service.CinemaService;
import com.backend.backend.service.UserService; // 用于获取当前用户 (模拟)
import com.backend.backend.util.SecurityUtil; // 导入 SecurityUtil
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "影院信息管理", description = "包含多角色操作影院信息的接口")
@RestController
@RequestMapping("/api")
public class CinemaController {

    @Autowired
    private CinemaService cinemaService;

    @Autowired
    private UserService userService; // 模拟获取当前用户信息

    // --- 影院管理员接口 ---

    @Tag(name = "影院管理员 - 影院管理")
    @Operation(
        summary = "申请新影院", 
        description = "影院管理员提交影院信息以供审核。申请后影院会处于待审核(PENDING_APPROVAL)状态，需等待系统管理员审核。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "成功创建影院申请", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"星光影院\",\n" +
                           "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "  \"phone\": \"010-12345678\",\n" +
                           "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
                           "  \"adminUserId\": 10,\n" +
                           "  \"status\": \"PENDING_APPROVAL\",\n" +
                           "  \"createTime\": \"2023-05-01T14:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-01T14:30:45\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如必填字段缺失或信息不完整",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"影院名称不能为空\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限")
    })
    @PostMapping("/cinema-admin/cinemas/apply")
    public ResponseEntity<?> applyForCinema(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "影院基本信息",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Cinema.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"name\": \"星光影院\",\n" +
                               "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                               "  \"phone\": \"010-12345678\",\n" +
                               "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                               "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody Cinema cinema) {
        Long currentUserId = SecurityUtil.getCurrentUserId(); // 获取当前用户 ID
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        try {
            Cinema createdCinema = cinemaService.applyForCinema(cinema, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCinema);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Tag(name = "影院管理员 - 影院管理")
    @Operation(
        summary = "获取自己管理的影院信息", 
        description = "获取当前影院管理员账户关联的影院信息。如果未申请影院或影院申请未批准，将返回404。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影院信息", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"星光影院\",\n" +
                           "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "  \"phone\": \"010-12345678\",\n" +
                           "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
                           "  \"adminUserId\": 10,\n" +
                           "  \"status\": \"APPROVED\",\n" +
                           "  \"createTime\": \"2023-05-01T14:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-02T10:15:23\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限"),
        @ApiResponse(responseCode = "404", description = "未找到影院信息，可能是尚未申请或申请未批准")
    })
    @GetMapping("/cinema-admin/cinema")
    public ResponseEntity<?> getMyCinema() {
         Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        Cinema cinema = cinemaService.getCinemaByAdminUserId(currentUserId);
        if (cinema == null) {
            return ResponseEntity.notFound().build(); // 可能未申请或未批准
        }
        return ResponseEntity.ok(cinema);
    }

    @Tag(name = "影院管理员 - 影院管理")
    @Operation(
        summary = "更新自己管理的影院信息", 
        description = "更新当前影院管理员账户关联的影院信息。仅当影院状态为APPROVED时才能更新。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功更新影院信息", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或更新失败",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"更新失败，影院状态非法\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(
            responseCode = "403", 
            description = "权限不足，可能是非影院管理员或尝试修改非自己管理的影院",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"无权修改该影院或影院不存在\",\n" +
                           "  \"code\": 403\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "未找到影院信息")
    })
    @PutMapping("/cinema-admin/cinema")
    public ResponseEntity<?> updateMyCinema(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "要更新的影院信息，ID必须匹配当前管理的影院",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Cinema.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"id\": 1,\n" +
                               "  \"name\": \"星光国际影院\",\n" +
                               "  \"address\": \"北京市海淀区中关村大街1号科技广场B座\",\n" +
                               "  \"phone\": \"010-87654321\",\n" +
                               "  \"logo\": \"https://example.com/logos/starlight-new.png\",\n" +
                               "  \"description\": \"全新升级的星光国际影院，拥有IMAX厅和全新的4D体验厅。\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody Cinema cinema) {
         Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        try {
            // Service 层会进行权限和状态校验，这里 Controller 层也加一层校验
            Cinema managedCinema = cinemaService.getCinemaByAdminUserId(currentUserId);
            if (managedCinema == null || !managedCinema.getId().equals(cinema.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权修改该影院或影院不存在");
            }
            boolean success = cinemaService.updateCinemaByAdmin(cinema, currentUserId);
            return success ? ResponseEntity.ok(cinema) : ResponseEntity.badRequest().body("更新失败");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- 系统管理员接口 ---

    @Tag(name = "系统管理员 - 影院管理")
    @Operation(
        summary = "获取影院列表 (分页)", 
        description = "获取所有影院列表，可按状态筛选。系统管理员可以查看任何状态的影院。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影院列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"name\": \"星光影院\",\n" +
                           "      \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "      \"adminUserId\": 10,\n" +
                           "      \"status\": \"APPROVED\",\n" +
                           "      \"createTime\": \"2023-05-01T14:30:45\"\n" +
                           "    },\n" +
                           "    {\n" +
                           "      \"id\": 2,\n" +
                           "      \"name\": \"环球影城\",\n" +
                           "      \"address\": \"上海市浦东新区陆家嘴环路1000号\",\n" +
                           "      \"adminUserId\": 11,\n" +
                           "      \"status\": \"PENDING_APPROVAL\",\n" +
                           "      \"createTime\": \"2023-05-10T09:20:33\"\n" +
                           "    }\n" +
                           "  ],\n" +
                           "  \"total\": 2,\n" +
                           "  \"size\": 10,\n" +
                           "  \"current\": 1,\n" +
                           "  \"pages\": 1\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/admin/cinemas")
    public ResponseEntity<Page<Cinema>> listCinemasForAdmin(
            @Parameter(
                description = "当前页码", 
                example = "1", 
                schema = @Schema(type = "integer", minimum = "1"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(
                description = "每页条数", 
                example = "10", 
                schema = @Schema(type = "integer", minimum = "1", maximum = "100"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(
                description = "按影院状态筛选", 
                schema = @Schema(implementation = Cinema.CinemaStatus.class), 
                example = "PENDING_APPROVAL",
                in = ParameterIn.QUERY
            ) 
            @RequestParam(required = false) Cinema.CinemaStatus status) {
        Page<Cinema> page = new Page<>(current, size);
        LambdaQueryWrapper<Cinema> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Cinema::getStatus, status);
        }
        wrapper.orderByDesc(Cinema::getCreateTime); // 按创建时间排序
        Page<Cinema> resultPage = cinemaService.page(page, wrapper);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "系统管理员 - 影院管理")
    @Operation(
        summary = "获取指定影院详情", 
        description = "根据ID获取任意状态的影院详情。系统管理员可以查看任何状态的影院。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影院详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"星光影院\",\n" +
                           "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "  \"phone\": \"010-12345678\",\n" +
                           "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
                           "  \"adminUserId\": 10,\n" +
                           "  \"status\": \"PENDING_APPROVAL\",\n" +
                           "  \"createTime\": \"2023-05-01T14:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-01T14:30:45\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "影院不存在")
    })
    @GetMapping("/admin/cinemas/{id}")
    public ResponseEntity<Cinema> getCinemaByIdForAdmin(
            @Parameter(
                description = "影院 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
        Cinema cinema = cinemaService.getById(id);
        return cinema != null ? ResponseEntity.ok(cinema) : ResponseEntity.notFound().build();
    }

    @Tag(name = "系统管理员 - 影院管理")
    @Operation(
        summary = "审核影院申请", 
        description = "审核处于PENDING_APPROVAL状态的影院申请。批准操作会自动将关联用户的角色从USER更新为CINEMA_ADMIN。拒绝操作会解除用户关联。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "审核成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"星光影院\",\n" +
                           "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "  \"phone\": \"010-12345678\",\n" +
                           "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
                           "  \"adminUserId\": 10,\n" +
                           "  \"status\": \"APPROVED\",\n" +
                           "  \"createTime\": \"2023-05-01T14:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-02T10:15:23\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误、影院状态不是待审核状态、或更新用户角色失败", // 合并了可能的 400 错误
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"影院不是待审核状态 或 关联的用户ID无效 或 更新用户角色失败...\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "影院不存在")
        // 可以为 500 添加说明
    })
    @PutMapping("/admin/cinemas/{id}/review")
    public ResponseEntity<?> reviewCinema(
            @Parameter(
                description = "待审核的影院 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "审核请求 (注意：body 中的 adminUserId 字段在新流程下已无效，无需提供)", // 更新描述
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CinemaReviewRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"approved\": true\n" +
                               "}" // 移除 adminUserId 示例
                    )
                )
            )
            @RequestBody CinemaReviewRequest reviewRequest) {
         try {
             // 注意：reviewRequest 中的 adminUserId 在新逻辑下不再被 service 层使用
            Cinema reviewedCinema = cinemaService.reviewCinema(id, reviewRequest.isApproved(), reviewRequest.getAdminUserId());
            return ResponseEntity.ok(reviewedCinema);
        } catch (RuntimeException e) {
             // 细化错误处理，区分 400 和 500
            if (e.getMessage().contains("不处于待审核状态") || 
                e.getMessage().contains("关联的用户ID无效") || 
                e.getMessage().contains("数据错误") ||
                e.getMessage().contains("更新用户角色失败")) {
                 // 这些被视为客户端或数据问题，返回 400
                 return ResponseEntity.badRequest().body(e.getMessage());
            } else {
                // 其他未预期的 RuntimeException 视为服务器内部错误
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("审核影院时发生内部错误: " + e.getMessage());
            }
        }
    }

    @Tag(name = "系统管理员 - 影院管理")
    @Operation(
        summary = "更新影院状态", 
        description = "更新影院的状态，例如禁用(DISABLED)或重新启用(APPROVED)影院。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "状态更新成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或状态更新失败",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"无效的状态更新请求\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "影院不存在")
    })
    @PutMapping("/admin/cinemas/{id}/status")
    public ResponseEntity<?> updateCinemaStatus(
            @Parameter(
                description = "要更新状态的影院 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "状态更新请求",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CinemaStatusUpdateRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"status\": \"DISABLED\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody CinemaStatusUpdateRequest statusRequest) {
        try {
             boolean success = cinemaService.updateCinemaStatus(id, statusRequest.getStatus());
             return success ? ResponseEntity.noContent().build() : ResponseEntity.badRequest().body("更新状态失败");
         } catch (RuntimeException e) {
             return ResponseEntity.badRequest().body(e.getMessage());
         }
    }

     @Tag(name = "系统管理员 - 影院管理")
     @Operation(
         summary = "系统管理员更新影院信息", 
         description = "系统管理员直接更新影院信息，权限较大，可以修改任何影院信息。"
     )
     @ApiResponses({
         @ApiResponse(
             responseCode = "200", 
             description = "更新成功", 
             content = @Content(
                 mediaType = "application/json",
                 schema = @Schema(implementation = Cinema.class)
             )
         ),
         @ApiResponse(responseCode = "400", description = "请求参数错误或更新失败"),
         @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
         @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
         @ApiResponse(responseCode = "404", description = "影院不存在")
     })
     @PutMapping("/admin/cinemas/{id}")
     public ResponseEntity<?> updateCinemaBySystemAdmin(
             @Parameter(
                 description = "要更新的影院 ID", 
                 required = true,
                 example = "1",
                 schema = @Schema(type = "integer", format = "int64")
             ) 
             @PathVariable Long id,
             
             @io.swagger.v3.oas.annotations.parameters.RequestBody(
                 description = "更新后的影院信息",
                 required = true,
                 content = @Content(
                     mediaType = "application/json",
                     schema = @Schema(implementation = Cinema.class),
                     examples = @ExampleObject(
                         value = "{\n" +
                                "  \"name\": \"星光国际影院\",\n" +
                                "  \"address\": \"北京市海淀区中关村大街1号科技广场B座\",\n" +
                                "  \"phone\": \"010-87654321\",\n" +
                                "  \"logo\": \"https://example.com/logos/starlight-new.png\",\n" +
                                "  \"description\": \"全新升级的星光国际影院，拥有IMAX厅和全新的4D体验厅。\",\n" +
                                "  \"adminUserId\": 15,\n" +
                                "  \"status\": \"APPROVED\"\n" +
                                "}"
                     )
                 )
             )
             @RequestBody Cinema cinema) {
         cinema.setId(id);
         // 系统管理员可以修改更多信息，但 Service 层可能需要额外方法或参数来区分
         boolean success = cinemaService.updateById(cinema); // 直接调用基础更新
         return success ? ResponseEntity.ok(cinema) : ResponseEntity.badRequest().build();
     }

    // --- 普通用户接口 ---

    @Tag(name = "用户 - 影院浏览")
    @Operation(
        summary = "获取已批准的影院列表 (分页)", 
        description = "获取状态为APPROVED的影院列表，普通用户只能查看已批准的影院。支持按名称、地址和特色功能筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取已批准的影院列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"name\": \"星光影院\",\n" +
                           "      \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "      \"phone\": \"010-12345678\",\n" +
                           "      \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "      \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
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
        )
    })
    @GetMapping("/cinemas")
    public ResponseEntity<Page<Cinema>> listApprovedCinemas(
            @Parameter(
                description = "当前页码", 
                example = "1", 
                schema = @Schema(type = "integer", minimum = "1"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(
                description = "每页条数", 
                example = "10", 
                schema = @Schema(type = "integer", minimum = "1", maximum = "100"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(
                description = "影院名称 (模糊匹配)",
                example = "星光",
                schema = @Schema(type = "string"),
                in = ParameterIn.QUERY
            )
            @RequestParam(required = false) String name,
            
            @Parameter(
                description = "地址关键词 (模糊匹配)",
                example = "北京",
                schema = @Schema(type = "string"),
                in = ParameterIn.QUERY
            )
            @RequestParam(required = false) String location,
            
            @Parameter(
                description = "特色功能 (模糊匹配，如IMAX, 杜比全景, 4D, 3D)",
                example = "IMAX",
                schema = @Schema(type = "string"),
                in = ParameterIn.QUERY
            )
            @RequestParam(required = false) String feature) {

        // 直接调用service层的增强方法
        Page<Cinema> resultPage = cinemaService.listApprovedCinemas(current, size, name, location, feature);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "用户 - 影院浏览")
    @Operation(
        summary = "获取已批准的影院详情", 
        description = "根据ID获取状态为APPROVED的影院详情，普通用户只能查看已批准的影院。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影院详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"name\": \"星光影院\",\n" +
                           "  \"address\": \"北京市海淀区中关村大街1号\",\n" +
                           "  \"phone\": \"010-12345678\",\n" +
                           "  \"logo\": \"https://example.com/logos/starlight.png\",\n" +
                           "  \"description\": \"位于市中心的现代化影院，拥有6个放映厅，提供舒适的观影体验。\",\n" +
                           "  \"status\": \"APPROVED\",\n" +
                           "  \"createTime\": \"2023-05-01T14:30:45\",\n" +
                           "  \"updateTime\": \"2023-05-02T10:15:23\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "影院不存在或未批准")
    })
    @GetMapping("/cinemas/{id}")
    public ResponseEntity<Cinema> getApprovedCinemaById(
            @Parameter(
                description = "影院 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
        Cinema cinema = cinemaService.getById(id);
        // 只返回已批准的影院
        if (cinema != null && cinema.getStatus() == Cinema.CinemaStatus.APPROVED) {
            return ResponseEntity.ok(cinema);
        }
        return ResponseEntity.notFound().build();
    }

    @Tag(name = "用户 - 影院入驻申请")
    @Operation(
        summary = "提交影院合作入驻申请",
        description = "普通用户提交新的影院合作申请信息。提交后，状态为待审核，需要系统管理员审核。",
        security = @SecurityRequirement(name = "JWT") // 表明需要 JWT 认证
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "申请提交成功",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Cinema.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 123,\n" +
                           "  \"name\": \"未来影城\",\n" +
                           "  \"address\": \"上海市浦东新区世纪大道100号\",\n" +
                           "  \"phone\": \"021-88888888\",\n" +
                           "  \"logo\": \"https://example.com/logos/future-cinema.png\",\n" +
                           "  \"description\": \"配备最新激光放映设备\",\n" +
                           "  \"adminUserId\": 5,\n" +
                           "  \"status\": \"PENDING_APPROVAL\",\n" +
                           "  \"createTime\": \"2024-07-28T10:00:00\",\n" +
                           "  \"updateTime\": \"2024-07-28T10:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "请求参数无效 (如必填项缺失)",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"影院名称不能为空\", \"code\": 400}"))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "未授权，用户未登录",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"用户未登录\", \"code\": 401}"))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "权限不足，用户不是普通用户",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"只有普通用户才能申请影院合作\", \"code\": 403}"))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "冲突，用户已有影院或申请",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"message\": \"您已有关联的影院或正在审核中的申请\", \"code\": 409}"))
        )
    })
    @PostMapping("/cinemas/partnership-application")
    @PreAuthorize("hasRole('USER')") // 确保只有 USER 角色的用户可以访问
    public ResponseEntity<?> handlePartnershipApplication(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "影院合作申请信息",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CinemaPartnershipApplicationRequestDTO.class)
                )
            )
            @Valid @RequestBody CinemaPartnershipApplicationRequestDTO applicationRequestDTO) {
        
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            // 虽然 PreAuthorize 应该已经处理了，但作为额外的安全层
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录"); 
        }

        try {
            Cinema createdCinema = cinemaService.handlePartnershipApplication(applicationRequestDTO, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCinema);
        } catch (RuntimeException e) {
            // 增强错误处理：更精确地匹配 Service 抛出的异常信息
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("只有普通用户才能申请")) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMessage);
                } else if (errorMessage.contains("已有关联的影院或正在审核中的申请")) {
                    // 明确捕获冲突错误
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage); 
                } else if (errorMessage.contains("数据错误：") || errorMessage.contains("关联的用户ID无效")) {
                    // Service 层的数据校验或查找错误，视为 Bad Request
                     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
                } 
                // 可以根据需要添加对 jakarta.validation.ConstraintViolationException 的处理，如果 @Valid 校验失败
            }
             // 其他未明确处理的 RuntimeException，视为内部错误
            // 或者，如果是数据库约束异常，可以尝试判断 SQLException 类型，但通常包装在 RuntimeException 里
            // log.error("处理影院申请时发生未知错误", e); // 记录详细错误日志
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("提交申请时发生内部错误"); // 对外隐藏具体错误细节
        }
    }

    // --- DTOs (需要在 dto 包下创建) ---
    // DTO for Cinema Review
    // @Data
    // public static class CinemaReviewRequest {
    //     private boolean approved;
    //     private Long adminUserId; // Optional: associate admin during approval
    // }
    // DTO for Cinema Status Update
    // @Data
    // public static class CinemaStatusUpdateRequest {
    //     private Cinema.CinemaStatus status;
    // }
} 