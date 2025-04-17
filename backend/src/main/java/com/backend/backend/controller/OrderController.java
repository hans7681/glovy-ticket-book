package com.backend.backend.controller;

import com.backend.backend.dto.CreateOrderRequest;
import com.backend.backend.entity.Order;
import com.backend.backend.entity.Cinema;
import com.backend.backend.service.OrderService;
import com.backend.backend.service.CinemaService;
import com.backend.backend.util.SecurityUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.backend.backend.exception.InvalidOperationException;
import com.backend.backend.exception.InvalidScreeningStatusException;
import com.backend.backend.exception.ResourceNotFoundException;
import com.backend.backend.exception.ScreeningNotFoundException;
import com.backend.backend.exception.SeatLockExpiredException;
import com.backend.backend.exception.SeatsUnavailableException;
import com.backend.backend.vo.OrderVO;
import com.backend.backend.entity.Room;
import com.backend.backend.service.RoomService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.backend.backend.entity.Screening;
import com.backend.backend.entity.Movie;
import com.backend.backend.entity.OrderSeat;

@Tag(name = "订单管理", description = "包含用户创建/查询/取消订单、管理员查询订单的接口")
@RestController
@RequestMapping("/api")
@Slf4j
@SecurityRequirement(name = "JWT")
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CinemaService cinemaService;
    
    @Autowired
    private RoomService roomService;

    // --- 用户接口 ---
    @Tag(name = "用户 - 订单操作")
    @Operation(
        summary = "创建新订单", 
        description = "用户选择场次和座位后创建新订单。订单创建后状态为 PENDING_PAYMENT。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", 
            description = "订单创建成功", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = OrderVO.class)
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "请求参数错误 (如座位信息无效、场次状态不允许购票)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "未授权，需要登录",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "场次不存在",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "资源冲突 (座位锁定失效或已被购买)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "服务器内部错误",
            content = @Content
        )
    })
    @PostMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderVO> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        log.info("Received request to create order from user: {} for screening: {}", userId, request.getScreeningId());

        OrderVO createdOrder = orderService.createOrder(request, userId);

        log.info("Order {} created successfully.", createdOrder.getOrderNo());
        // Return 201 Created with the created order details
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @Tag(name = "用户 - 订单操作")
    @Operation(
        summary = "获取我的订单列表", 
        description = "分页获取当前用户的订单列表，可按状态筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取订单列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class),
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"records\": [\n" +
                           "    {\n" +
                           "      \"id\": 1,\n" +
                           "      \"orderNo\": \"20240521103015001\",\n" +
                           "      \"userId\": 5,\n" +
                           "      \"screeningId\": 1,\n" +
                           "      \"cinemaId\": 1,\n" +
                           "      \"movieId\": 1,\n" +
                           "      \"movieTitle\": \"功夫\",\n" +
                           "      \"cinemaName\": \"星光影院\",\n" +
                           "      \"screeningTime\": \"2025-01-30T10:00:00\",\n" +
                           "      \"totalPrice\": 90.00,\n" +
                           "      \"status\": \"PAID\",\n" +
                           "      \"createTime\": \"2024-05-21T10:30:15\"\n" +
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
        @ApiResponse(responseCode = "401", description = "未授权，需要登录")
    })
    @GetMapping("/orders")
    public ResponseEntity<?> listMyOrders(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按订单状态筛选", schema = @Schema(implementation = Order.OrderStatus.class), example = "PAID", in = ParameterIn.QUERY)
            @RequestParam(required = false) Order.OrderStatus status) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        Page<Order> page = new Page<>(current, size);
        Page<Order> resultPage = orderService.listUserOrders(page, currentUserId, status);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "用户 - 订单操作")
    @Operation(
        summary = "获取我的订单详情", 
        description = "根据订单ID(数字)或订单号(字符串)获取当前用户的订单详情，包含电影、影院、场次、座位等详细信息。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取订单详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Order.class), // 示例与创建订单类似，但包含更多关联信息
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"id\": 1,\n" +
                           "  \"orderNo\": \"20240521103015001\",\n" +
                           "  \"userId\": 5,\n" +
                           "  \"screeningId\": 1,\n" +
                           "  \"cinemaId\": 1,\n" +
                           "  \"movieId\": 1,\n" +
                           "  \"movieTitle\": \"功夫\",\n" +
                           "  \"cinemaName\": \"星光影院\",\n" +
                           "  \"roomName\": \"1号厅\",\n" +
                           "  \"screeningTime\": \"2025-01-30T10:00:00\",\n" +
                           "  \"totalPrice\": 90.00,\n" +
                           "  \"status\": \"PAID\",\n" +
                           "  \"createTime\": \"2024-05-21T10:30:15\",\n" +
                           "  \"payTime\": \"2024-05-21T10:35:00\",\n" +
                           "  \"orderSeats\": [\n" +
                           "    { \"id\": 1, \"orderId\": 1, \"rowIndex\": 5, \"colIndex\": 7, \"seatLabel\": \"5排7座\" },\n" +
                           "    { \"id\": 2, \"orderId\": 1, \"rowIndex\": 5, \"colIndex\": 8, \"seatLabel\": \"5排8座\" }\n" +
                           "  ]\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "404", description = "订单不存在或不属于当前用户")
    })
    @GetMapping("/orders/{identifier}")
    public ResponseEntity<?> getMyOrderDetails(
            @Parameter(description = "订单ID (数字) 或订单号 (字符串)", required = true, example = "1", schema = @Schema(type = "string")) 
            @PathVariable String identifier) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        Object parsedIdentifier;
        try {
            parsedIdentifier = Long.parseLong(identifier);
        } catch (NumberFormatException e) {
            parsedIdentifier = identifier;
        }
        // Service 层会校验权限
        Order order = orderService.getOrderDetails(parsedIdentifier, currentUserId);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @Tag(name = "用户 - 订单操作")
    @Operation(
        summary = "取消我的订单", 
        description = "用户取消处于待支付(PENDING_PAYMENT)状态的订单。其他状态的订单无法取消。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "订单取消成功"),
        @ApiResponse(
            responseCode = "400", 
            description = "取消失败，订单状态不允许取消",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = "{\n" +
                           "  \"message\": \"订单已支付，无法取消\",\n" +
                           "  \"code\": 400\n" +
                           "}"
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "无权操作该订单"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/orders/{identifier}/cancel")
    public ResponseEntity<?> cancelMyOrder(
             @Parameter(description = "订单ID (数字) 或订单号 (字符串)", required = true, example = "1", schema = @Schema(type = "string")) 
             @PathVariable String identifier) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
        if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }
        // Service 层会校验订单是否属于该用户以及是否可取消
        try {
            // 先解析 identifier 获取订单 ID，并校验权限
            Order order = orderService.getOrderDetails(identifier, currentUserId);
            if (order == null) {
                // getOrderDetails 内部应处理权限，这里理论上不会执行，但加上更保险
                return ResponseEntity.notFound().build(); 
            }
            boolean success = orderService.cancelOrder(order.getId(), currentUserId);
            // cancelOrder 内部也应校验状态和权限
            return success ? ResponseEntity.noContent().build() : ResponseEntity.badRequest().body("取消失败或状态不允许");
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

    @Tag(name = "用户 - 订单操作")
    @Operation(
        summary = "(模拟)标记订单为已支付", 
        description = "将指定订单的状态从 PENDING_PAYMENT 更新为 PAID。用户只能操作自己的订单。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "订单状态更新成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Order.class))), // Return updated order
        @ApiResponse(responseCode = "400", description = "操作失败，订单状态不正确或无效操作"),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "无权操作该订单"), // If ownership check fails inside service
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @PutMapping("/orders/{identifier}/mark-as-paid")
    @PreAuthorize("isAuthenticated()") // User must be logged in
    public ResponseEntity<?> markOrderAsPaid(
            @Parameter(description = "订单ID (数字) 或订单号 (字符串)", required = true, example = "1", schema = @Schema(type = "string")) 
            @PathVariable String identifier) {
                
        Long currentUserId = SecurityUtil.getCurrentUserId();
        // No need to check null here as @PreAuthorize handles it, but good practice
        if (currentUserId == null) { 
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse("用户未认证"));
        }

        try {
            // Service method handles identifier parsing and ownership check
            Order updatedOrder = orderService.markOrderAsPaid(identifier, currentUserId); 
            return ResponseEntity.ok(updatedOrder);
        } catch (ResourceNotFoundException e) {
            log.warn("Mark as paid failed for user {}: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getMessage()));
        } catch (InvalidOperationException e) {
            log.warn("Mark as paid failed for user {}: {}", currentUserId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error marking order as paid for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse("标记订单为已支付时发生内部错误"));
        }
    }

    // --- 管理员接口 ---
    @Tag(name = "系统管理员 - 订单管理")
    @Operation(
        summary = "获取所有订单列表 (分页)", 
        description = "系统管理员获取所有订单列表，可按用户、影院、状态筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取订单列表", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class) // 示例与用户类似
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限")
    })
    @GetMapping("/admin/orders")
    public ResponseEntity<Page<Order>> listAllOrdersForAdmin(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按用户 ID 筛选", example = "5", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY) 
            @RequestParam(required = false) Long userId,
            
            @Parameter(description = "按影院 ID 筛选", example = "1", schema = @Schema(type = "integer", format = "int64"), in = ParameterIn.QUERY) 
            @RequestParam(required = false) Long cinemaId,
            
            @Parameter(description = "按订单状态筛选", schema = @Schema(implementation = Order.OrderStatus.class), example = "PENDING_PAYMENT", in = ParameterIn.QUERY)
            @RequestParam(required = false) Order.OrderStatus status) {
        // 权限由 SecurityConfig 控制
        Page<Order> page = new Page<>(current, size);
        Page<Order> resultPage = orderService.listAllOrders(page, userId, cinemaId, status);
        return ResponseEntity.ok(resultPage);
    }

    @Tag(name = "系统管理员 - 订单管理")
    @Operation(
        summary = "获取订单详情 (管理员)", 
        description = "系统管理员根据订单ID(数字)或订单号(字符串)获取订单详情。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取订单详情", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Order.class) // 示例与用户类似
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足，需要系统管理员权限"),
        @ApiResponse(responseCode = "404", description = "订单不存在")
    })
    @GetMapping("/admin/orders/{identifier}")
    public ResponseEntity<?> getOrderDetailsForAdmin(
             @Parameter(description = "订单ID (数字) 或订单号 (字符串)", required = true, example = "1", schema = @Schema(type = "string")) 
             @PathVariable String identifier) {
        // 权限由 SecurityConfig 控制
        Object parsedIdentifier;
        try {
            parsedIdentifier = Long.parseLong(identifier);
        } catch (NumberFormatException e) {
            parsedIdentifier = identifier;
        }
        // 管理员查询时不传入 userId 进行权限校验
        Order order = orderService.getOrderDetails(parsedIdentifier, null);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    // --- 影院管理员接口 ---
    @Tag(name = "影院管理员 - 订单管理")
    @Operation(
        summary = "获取本影院订单列表 (分页)", 
        description = "影院管理员获取其管理的影院的订单列表，可按状态筛选。"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "成功获取订单列表 (返回包含 OrderVO 的分页对象)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
        @ApiResponse(responseCode = "403", description = "权限不足或未关联影院")
    })
    @GetMapping("/cinema-admin/orders")
    public ResponseEntity<?> listCinemaOrdersForAdmin(
            @Parameter(description = "当前页码", example = "1", schema = @Schema(minimum = "1"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "1") int current,
            
            @Parameter(description = "每页条数", example = "10", schema = @Schema(minimum = "1", maximum = "100"), in = ParameterIn.QUERY) 
            @RequestParam(defaultValue = "10") int size,
            
            @Parameter(description = "按订单状态筛选", schema = @Schema(implementation = Order.OrderStatus.class), example = "PAID", in = ParameterIn.QUERY)
            @RequestParam(required = false) Order.OrderStatus status) {
        Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户未登录")); // Return structured error
         }

         // 1. Get the cinema managed by the current admin user
         Cinema managedCinema = cinemaService.getCinemaByAdminUserId(currentUserId);
         if (managedCinema == null) {
             // Use a more specific error message or potentially a custom exception if preferred
             return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前用户未关联任何影院或影院信息不存在"));
         }
         // Optional: Check cinema status if admins should only access orders of APPROVED cinemas
         // if (managedCinema.getStatus() != Cinema.CinemaStatus.APPROVED) {
         //     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "影院状态非正常，无法查询订单"));
         // }

         Long managedCinemaId = managedCinema.getId();
         String managedCinemaName = managedCinema.getName();
        
        Page<Order> page = new Page<>(current, size);
        try {
            // Pass the managedCinemaId to the service layer
            Page<Order> orderPage = orderService.listCinemaOrders(page, managedCinemaId, currentUserId, status);
            
            // Convert Page<Order> to Page<OrderVO>
            IPage<OrderVO> orderVoPage = orderPage.convert(order -> {
                OrderVO vo = new OrderVO();
                // Basic order info (copy properties or map manually)
                vo.setOrderId(order.getId());
                vo.setOrderNo(order.getOrderNo());
                vo.setUserId(order.getUserId());
                vo.setScreeningId(order.getScreeningId());
                vo.setMovieId(order.getMovieId());
                vo.setTotalAmount(order.getTotalAmount());
                vo.setSeatCount(order.getSeatCount());
                vo.setStatus(order.getStatus());
                vo.setPaymentTime(order.getPaymentTime());
                vo.setCancelTime(order.getCancelTime());
                vo.setCreateTime(order.getCreateTime());
                vo.setCinemaId(order.getCinemaId());
                
                // Add Cinema Name
                vo.setCinemaName(managedCinemaName);
                
                // Add Room Name and other related info if screening exists
                Screening screening = order.getScreening(); // Assuming service already fetches this
                if (screening != null) {
                    vo.setScreeningStartTime(screening.getStartTime());
                    // Fetch Room details if needed
                    Room room = roomService.getById(screening.getRoomId());
                    if (room != null) {
                        vo.setRoomName(room.getName());
                    }
                    // Fetch Movie title if needed
                    Movie movie = order.getMovie(); // Assuming service already fetches this
                    if (movie != null) {
                         vo.setMovieTitle(movie.getTitle());
                    }
                }
                // Add seats description if seats exist
                 List<OrderSeat> seats = order.getSeats(); // Assuming service fetches this
                 if (seats != null && !seats.isEmpty()) {
                     vo.setSeatsDescription(seats.stream()
                                               .map(OrderSeat::getSeatLabel)
                                               .collect(Collectors.toList()));
                 }

                return vo;
            });
            
            // Return the converted page, ensuring the type matches the ResponseEntity signature
            // If the return type MUST be Page<OrderVO>, we might need an extra step:
            Page<OrderVO> finalPage = new Page<>(orderVoPage.getCurrent(), orderVoPage.getSize(), orderVoPage.getTotal());
            finalPage.setRecords(orderVoPage.getRecords());
            finalPage.setPages(orderVoPage.getPages());
            
            return ResponseEntity.ok(finalPage);
        } catch (RuntimeException e) { // Keep generic catch or refine based on expected service exceptions
             log.error("Error listing cinema orders for admin {} and cinema {}: {}", currentUserId, managedCinemaId, e.getMessage(), e); // Log the error
             // The service layer check should ideally prevent RuntimeException for permission issues now,
             // but keep a generic handler for other potential issues.
             // Consider mapping specific service exceptions (like ResourceNotFound) to appropriate HTTP statuses if needed.
             // If the service still throws "无权查看该影院的订单", it indicates an issue within the service layer itself.
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "查询订单时发生内部错误: " + e.getMessage()));
        }
    }

     @Tag(name = "影院管理员 - 订单管理")
     @Operation(
         summary = "获取订单详情 (影院管理员)", 
         description = "影院管理员根据订单ID(数字)或订单号(字符串)获取其影院的订单详情。"
     )
     @ApiResponses({
         @ApiResponse(
             responseCode = "200", 
             description = "成功获取订单详情", 
             content = @Content(
                 mediaType = "application/json",
                 schema = @Schema(implementation = OrderVO.class)
             )
         ),
         @ApiResponse(responseCode = "401", description = "未授权，需要登录"),
         @ApiResponse(responseCode = "403", description = "权限不足或无权查看该订单"),
         @ApiResponse(responseCode = "404", description = "订单不存在")
     })
     @GetMapping("/cinema-admin/orders/{identifier}")
     public ResponseEntity<?> getOrderDetailsForCinemaAdmin(
             @Parameter(description = "订单ID (数字) 或订单号 (字符串)", required = true, example = "1", schema = @Schema(type = "string")) 
             @PathVariable String identifier) {
         Long currentUserId = SecurityUtil.getCurrentUserId();
         if (currentUserId == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
         }

         Object parsedIdentifier;
         try {
             parsedIdentifier = Long.parseLong(identifier);
         } catch (NumberFormatException e) {
             parsedIdentifier = identifier;
         }

         try {
             // 1. 获取订单详情 (不校验用户)
             Order order = orderService.getOrderDetails(parsedIdentifier, null);
             if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("订单不存在");
             }

             // 2. 获取当前影院管理员管理的影院
             Cinema managedCinema = cinemaService.getCinemaByAdminUserId(currentUserId);
             if (managedCinema == null) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("当前用户未关联任何影院");
             }
             
             // 3. 校验订单是否属于该影院
             if (!Objects.equals(order.getCinemaId(), managedCinema.getId())) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权查看该订单，订单不属于您管理的影院");
             }

             // 4. 校验通过，组装 OrderVO 并返回
            OrderVO vo = new OrderVO();
             // Basic order info
            vo.setOrderId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setUserId(order.getUserId());
            vo.setScreeningId(order.getScreeningId());
            vo.setMovieId(order.getMovieId());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setSeatCount(order.getSeatCount());
            vo.setStatus(order.getStatus());
            vo.setPaymentTime(order.getPaymentTime());
            vo.setCancelTime(order.getCancelTime());
            vo.setCreateTime(order.getCreateTime());
            vo.setCinemaId(order.getCinemaId());
            
            // Add Cinema Name
            vo.setCinemaName(managedCinema.getName());
            
             // Add Room Name and other related info if screening exists
            Screening screening = order.getScreening(); // Assuming service fetches this
            if (screening != null) {
                 vo.setScreeningStartTime(screening.getStartTime());
                 Room room = roomService.getById(screening.getRoomId());
                 if (room != null) {
                     vo.setRoomName(room.getName());
                 }
                Movie movie = order.getMovie(); // Assuming service fetches this
                 if (movie != null) {
                     vo.setMovieTitle(movie.getTitle());
                 }
            }
            
            // Add seats description
            List<OrderSeat> seats = order.getSeats(); // Assuming service fetches this
            if (seats != null && !seats.isEmpty()) {
                vo.setSeatsDescription(seats.stream()
                                           .map(OrderSeat::getSeatLabel)
                                           .collect(Collectors.toList()));
            }

             return ResponseEntity.ok(vo);
             
         } catch (RuntimeException e) {
             // 处理其他潜在的运行时异常，虽然理论上前面的检查覆盖了主要情况
             // 可以记录日志 e.g., log.error("Error fetching order details for cinema admin", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取订单详情时发生内部错误");
         }
     }

    private Map<String, String> errorResponse(String message) {
        return Collections.singletonMap("message", message);
    }

    @ExceptionHandler(ScreeningNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleScreeningNotFound(ScreeningNotFoundException ex) {
        log.warn("Order creation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(InvalidScreeningStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvalidScreeningStatus(InvalidScreeningStatusException ex) {
        log.warn("Order creation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SeatLockExpiredException.class)
    public ResponseEntity<Map<String, String>> handleSeatLockExpired(SeatLockExpiredException ex) {
        log.warn("Order creation failed due to seat lock issue: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
    
    @ExceptionHandler(SeatsUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleSeatsUnavailable(SeatsUnavailableException ex) {
        log.warn("Order creation failed because seats are unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
}