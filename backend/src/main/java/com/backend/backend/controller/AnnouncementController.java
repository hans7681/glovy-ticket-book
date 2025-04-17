package com.backend.backend.controller;

import com.backend.backend.dto.PublishRequest; // 需要创建
import com.backend.backend.entity.Announcement;
import com.backend.backend.service.AnnouncementService;
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
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.backend.backend.util.SecurityUtil; // 导入

import java.time.LocalDateTime;

@Tag(name = "公告管理", description = "包含管理员和用户查看公告的接口")
@RestController
@RequestMapping("/api")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    // --- 系统管理员接口 ---
    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "添加新公告", 
        description = "添加新的系统公告。新创建的公告默认为未发布状态，需要额外调用发布接口才能使其对普通用户可见。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "成功创建公告", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Announcement.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"title\": \"系统维护通知\",\n" +
                           "  \"content\": \"系统将于2023年2月1日凌晨2点进行维护升级，预计2小时。\",\n" +
                           "  \"publisherId\": 1,\n" +
                           "  \"isPublished\": false,\n" +
                           "  \"publishTime\": null,\n" +
                           "  \"createTime\": \"2023-01-30T14:25:33\",\n" +
                           "  \"updateTime\": \"2023-01-30T14:25:33\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @PostMapping("/admin/announcements")
    public ResponseEntity<Announcement> addAnnouncement(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "公告信息，必须包含标题和内容。其他字段如发布状态会被系统自动设置。",
                required = true,
                content = @Content(
                    mediaType = "application/json", 
                    schema = @Schema(implementation = Announcement.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"title\": \"系统维护通知\",\n" +
                               "  \"content\": \"系统将于2023年2月1日凌晨2点进行维护升级，预计2小时。\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody Announcement announcement) {
            
        // 从 SecurityContext 获取当前登录用户 ID
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        announcement.setPublisherId(currentUserId);
        announcement.setIsPublished(false); // 默认不发布
        announcement.setPublishTime(null);
        boolean success = announcementService.save(announcement);
        return success ? ResponseEntity.status(HttpStatus.CREATED).body(announcement) : ResponseEntity.badRequest().build();
    }

    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "更新公告信息", 
        description = "更新指定ID的公告内容。只能修改标题和内容，其他属性如发布状态不会被修改。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "更新成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Announcement.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @PutMapping("/admin/announcements/{id}")
    public ResponseEntity<Announcement> updateAnnouncement(
            @Parameter(
                description = "公告 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "需要更新的公告信息，只有标题和内容会被更新。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Announcement.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"title\": \"系统维护通知（已更新）\",\n" +
                               "  \"content\": \"系统维护时间已调整为2023年2月2日凌晨3点，预计1小时。\"\n" +
                               "}"
                    )
                )
            )
            @RequestBody Announcement announcement) {
            
        // 检查当前用户是否已登录
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
            
        announcement.setId(id);
        // 防止关键信息被随意修改
        Announcement existing = announcementService.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        announcement.setPublisherId(existing.getPublisherId()); // 保持原有发布者
        announcement.setIsPublished(existing.getIsPublished()); // 保持原有发布状态
        announcement.setPublishTime(existing.getPublishTime()); // 保持原有发布时间

        boolean success = announcementService.updateById(announcement);
        return success ? ResponseEntity.ok(announcement) : ResponseEntity.badRequest().build();
    }

    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "删除公告", 
        description = "删除指定ID的公告。注意：此操作不可恢复。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @DeleteMapping("/admin/announcements/{id}")
    public ResponseEntity<Void> deleteAnnouncement(
            @Parameter(
                description = "公告 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
            
        // 检查当前用户是否已登录
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
            
        boolean success = announcementService.removeById(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "获取公告列表 (分页)", 
        description = "获取所有公告列表，包括已发布和未发布的。可通过查询参数筛选只返回已发布或未发布的公告。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取公告列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"title\": \"系统维护通知\",\n" +
                           "      \"content\": \"系统将于2023年2月1日凌晨2点进行维护升级，预计2小时。\",\n" +
                           "      \"publisherId\": 1,\n" +
                           "      \"isPublished\": true,\n" +
                           "      \"publishTime\": \"2023-01-30T15:00:00\",\n" +
                           "      \"createTime\": \"2023-01-30T14:25:33\",\n" +
                           "      \"updateTime\": \"2023-01-30T15:00:00\"\n" +
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
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/admin/announcements")
    public ResponseEntity<Page<Announcement>> listAnnouncementsForAdmin(
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
                description = "按发布状态筛选 (true: 已发布, false: 未发布)", 
                example = "true", 
                schema = @Schema(type = "boolean"), 
                in = ParameterIn.QUERY
            ) 
            @RequestParam(required = false) Boolean isPublished) {
            
        // 检查当前用户是否已登录
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
            
        Page<Announcement> page = new Page<>(current, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (isPublished != null) {
            wrapper.eq(Announcement::getIsPublished, isPublished);
        }
        wrapper.orderByDesc(Announcement::getCreateTime); // 按创建时间排序
        Page<Announcement> resultPage = announcementService.page(page, wrapper);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "获取公告详情", 
        description = "根据ID获取单个公告详情。系统管理员可以查看任何状态的公告详情。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取公告详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Announcement.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"title\": \"系统维护通知\",\n" +
                           "  \"content\": \"系统将于2023年2月1日凌晨2点进行维护升级，预计2小时。\",\n" +
                           "  \"publisherId\": 1,\n" +
                           "  \"isPublished\": true,\n" +
                           "  \"publishTime\": \"2023-01-30T15:00:00\",\n" +
                           "  \"createTime\": \"2023-01-30T14:25:33\",\n" +
                           "  \"updateTime\": \"2023-01-30T15:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @GetMapping("/admin/announcements/{id}")
    public ResponseEntity<Announcement> getAnnouncementByIdForAdmin(
            @Parameter(
                description = "公告 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id) {
            
        // 检查当前用户是否已登录
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
            
        Announcement announcement = announcementService.getById(id);
        return announcement != null ? ResponseEntity.ok(announcement) : ResponseEntity.notFound().build();
    }

    @Tag(name = "系统管理员 - 公告管理")
    @Operation(
        summary = "发布/取消发布公告", 
        description = "更新公告的发布状态。发布时会记录当前时间为发布时间，取消发布时会清除发布时间。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "操作成功，返回更新后的公告", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Announcement.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "操作失败，可能是公告状态已经是目标状态",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"公告已经是发布状态\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "公告不存在")
    })
    @PutMapping("/admin/announcements/{id}/publish")
    public ResponseEntity<?> publishAnnouncement(
            @Parameter(
                description = "公告 ID", 
                required = true,
                example = "1",
                schema = @Schema(type = "integer", format = "int64")
            ) 
            @PathVariable Long id,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "发布状态请求体",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PublishRequest.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"publish\": true\n" +
                               "}"
                    )
                )
            )
            @RequestBody PublishRequest publishRequest) {
            
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        
        try {
            Announcement updatedAnnouncement = announcementService.publishAnnouncement(id, publishRequest.isPublish(), currentUserId);
            return ResponseEntity.ok(updatedAnnouncement);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- 普通用户接口 ---
    @Tag(name = "用户 - 公告查看")
    @Operation(
        summary = "获取已发布的公告列表", 
        description = "获取所有已发布的公告列表，按发布时间倒序排列。此接口不需要登录即可访问。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取已发布的公告列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"title\": \"系统维护通知\",\n" +
                           "      \"content\": \"系统将于2023年2月1日凌晨2点进行维护升级，预计2小时。\",\n" +
                           "      \"publisherId\": 1,\n" +
                           "      \"isPublished\": true,\n" +
                           "      \"publishTime\": \"2023-01-30T15:00:00\",\n" +
                           "      \"createTime\": \"2023-01-30T14:25:33\",\n" +
                           "      \"updateTime\": \"2023-01-30T15:00:00\"\n" +
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
    @GetMapping("/announcements")
    public ResponseEntity<Page<Announcement>> listPublishedAnnouncements(
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
            @RequestParam(defaultValue = "10") int size) {
            
        Page<Announcement> page = new Page<>(current, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Announcement::getIsPublished, true);
        wrapper.orderByDesc(Announcement::getPublishTime); // 按发布时间排序
        Page<Announcement> resultPage = announcementService.page(page, wrapper);
        return ResponseEntity.ok(resultPage);
    }

    // DTO for Publish Request
    // @Data
    // public static class PublishRequest {
    //     private boolean publish;
    // }
} 