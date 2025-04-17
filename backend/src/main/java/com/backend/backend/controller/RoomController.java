package com.backend.backend.controller;

import com.backend.backend.entity.Cinema;
import com.backend.backend.entity.Room;
import com.backend.backend.service.CinemaService;
import com.backend.backend.service.RoomService;
import com.backend.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;

@Tag(name = "影院管理员 - 影厅管理", description = "影院管理员管理其所属影院的影厅信息 (需要影院管理员权限)")
@RestController
@RequestMapping("/api/cinema-admin/rooms") // 影院管理员管理影厅的基础路径
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private CinemaService cinemaService; // 用于获取影院 ID

    @Operation(
        summary = "添加新影厅", 
        description = "向当前管理员所属的影院添加一个新的影厅。需要提供影厅名称、行数、列数。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "影厅创建成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Room.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"cinemaId\": 1,\n" +
                           "  \"name\": \"1号厅\",\n" +
                           "  \"rows\": 10,\n" +
                           "  \"cols\": 15,\n" +
                           "  \"createTime\": \"2024-05-20T15:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误，例如影厅名称重复或行列数无效",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"影厅名称已存在\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限或未关联影院")
    })
    @PostMapping
    public ResponseEntity<?> addRoom(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "影厅信息，cinemaId 会被后端根据当前管理员信息覆盖。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Room.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"name\": \"1号厅\",\n" +
                               "  \"rows\": 10,\n" +
                               "  \"cols\": 15\n" +
                               "}"
                    )
                )
            )
            @RequestBody Room room) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
        }
        try {
            // Service 层会通过 adminUserId 找到对应的 CinemaID 并进行校验和设置
            Room createdRoom = roomService.addRoom(room, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
        } catch (RuntimeException e) {
            // 根据 Service 异常判断 400 或 403
            // 更精确地判断权限或状态问题
             if (e.getMessage().contains("无权") || e.getMessage().contains("未批准") || e.getMessage().contains("未关联")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
             } else {
                 // 其他 RuntimeException (如名称重复、保存失败等) 视为 Bad Request
                 return ResponseEntity.badRequest().body(e.getMessage());
             }
        }
    }

    @Operation(
        summary = "获取影厅列表", 
        description = "获取当前管理员所属影院的所有影厅列表。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影厅列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(type = "array", implementation = Room.class),
                examples = @ExampleObject(
                    value = "[\n" +
                           "  {\n" +
                           "    \"id\": 1,\n" +
                           "    \"cinemaId\": 1,\n" +
                           "    \"name\": \"1号厅\",\n" +
                           "    \"rows\": 10,\n" +
                           "    \"cols\": 15\n" +
                           "  },\n" +
                           "  {\n" +
                           "    \"id\": 2,\n" +
                           "    \"cinemaId\": 1,\n" +
                           "    \"name\": \"IMAX厅\",\n" +
                           "    \"rows\": 8,\n" +
                           "    \"cols\": 12\n" +
                           "  }\n" +
                           "]"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要影院管理员权限或未关联影院")
    })
    @GetMapping
    public ResponseEntity<?> listMyRooms() {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        try {
            // Service 层需要通过 adminUserId 找到 cinemaId
             Cinema cinema = cinemaService.getCinemaByAdminUserId(currentUserId);
             if (cinema == null || cinema.getStatus() != Cinema.CinemaStatus.APPROVED) {
                 throw new RuntimeException("当前用户未关联已批准的影院");
             }
            List<Room> rooms = roomService.listRoomsByCinemaId(cinema.getId(), currentUserId);
            return ResponseEntity.ok(rooms);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @Operation(
        summary = "获取影厅详情", 
        description = "根据 ID 获取当前管理员所属影院的单个影厅详情。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取影厅详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Room.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"cinemaId\": 1,\n" +
                           "  \"name\": \"1号厅\",\n" +
                           "  \"rows\": 10,\n" +
                           "  \"cols\": 15,\n" +
                           "  \"createTime\": \"2024-05-20T15:00:00\",\n" +
                           "  \"updateTime\": \"2024-05-21T11:00:00\"\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或无权查看该影厅"),
        @ApiResponse(responseCode = "404", description = "影厅不存在")
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomById(
            @Parameter(description = "影厅 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long roomId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        try {
            Room room = roomService.getRoomById(roomId, currentUserId);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("不存在")) {
                 return ResponseEntity.notFound().build();
            } else {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            }
        }
    }

    @Operation(
        summary = "更新影厅信息", 
        description = "根据 ID 更新当前管理员所属影院的影厅信息，例如修改名称或座位布局。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "更新成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Room.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误或更新失败",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"更新失败，影厅名称重复\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或无权修改该影厅"),
        @ApiResponse(responseCode = "404", description = "影厅不存在")
    })
    @PutMapping("/{roomId}")
    public ResponseEntity<?> updateRoom(
            @Parameter(description = "要更新的影厅 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long roomId,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "更新后的影厅信息，roomId 和 cinemaId 会被后端覆盖或校验。",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = Room.class),
                    examples = @ExampleObject(
                        value = "{\n" +
                               "  \"name\": \"激光厅\",\n" +
                               "  \"rows\": 9,\n" +
                               "  \"cols\": 14\n" +
                               "}"
                    )
                )
            )
            @RequestBody Room room) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        room.setId(roomId);
        try {
            boolean success = roomService.updateRoom(room, currentUserId);
            // 成功后，重新获取一下更新后的 room 信息返回
             Room updatedRoom = success ? roomService.getRoomById(roomId, currentUserId) : null;
             return updatedRoom != null ? ResponseEntity.ok(updatedRoom) : ResponseEntity.badRequest().body("更新失败");
        } catch (RuntimeException e) {
             // 根据 Service 异常细化响应
              if (e.getMessage().contains("不存在")) {
                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
             } else if (e.getMessage().contains("无权")) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
             } else {
                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
             }
        }
    }

    @Operation(
        summary = "删除影厅", 
        description = "根据 ID 删除当前管理员所属影院的影厅。如果影厅有关联的未完成场次，可能无法删除。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "删除失败，例如影厅存在关联场次",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"影厅尚有关联场次，无法删除\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或无权删除该影厅"),
        @ApiResponse(responseCode = "404", description = "影厅不存在")
    })
    @DeleteMapping("/{roomId}")
    public ResponseEntity<?> deleteRoom(
            @Parameter(description = "要删除的影厅 ID", required = true, example = "1", schema = @Schema(type = "integer", format = "int64")) 
            @PathVariable Long roomId) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        try {
            boolean success = roomService.deleteRoom(roomId, currentUserId);
            return success ? ResponseEntity.noContent().build() : ResponseEntity.badRequest().body("删除失败");
        } catch (RuntimeException e) {
            // 根据 Service 异常细化响应
            if (e.getMessage().contains("不存在")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            } else if (e.getMessage().contains("无权")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
            }
        }
    }
} 