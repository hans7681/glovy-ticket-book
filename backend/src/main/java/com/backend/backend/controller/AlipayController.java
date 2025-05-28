package com.backend.backend.controller;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.backend.backend.config.AlipayConfig;
import com.backend.backend.entity.Order;
import com.backend.backend.mapper.OrderMapper;
import com.backend.backend.service.AlipayService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝支付控制器
 */
@RestController
@RequestMapping("/api/payment/alipay")
@Slf4j
@Tag(name = "支付宝支付相关接口", description = "包括创建支付、支付回调等接口")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 创建支付宝支付
     *
     * @param orderNo 订单号
     * @param frontendUrl 前端回调URL (可选，默认为localhost:5173)
     * @return 支付表单HTML
     */
    @PostMapping(value = "/create/{orderNo}", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "创建支付宝支付", description = "根据订单号创建支付宝支付订单，返回支付表单HTML")
    public String createPayment(
            @Parameter(description = "订单号", required = true)
            @PathVariable String orderNo,
            @Parameter(description = "前端回调URL，如http://localhost:5174", required = false)
            @RequestParam(required = false, defaultValue = "http://localhost:5173") String frontendUrl) throws Exception {
        log.info("创建支付宝支付，订单号：{}，前端回调URL：{}", orderNo, frontendUrl);
        return alipayService.createPayment(orderNo, frontendUrl);
    }

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝通知参数
     * @return 处理结果
     */
    @PostMapping("/notify")
    @Operation(summary = "支付宝异步通知", description = "接收并处理支付宝的支付结果通知")
    public String handleAlipayNotification(@RequestParam Map<String, String> params) {
        log.info("收到支付宝异步通知: {}", params);

        // 1. 验签
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType());
        } catch (AlipayApiException e) {
            log.error("支付宝通知验签失败", e);
            return "failure";
        }

        if (!signVerified) {
            log.warn("支付宝通知验签未通过");
            return "failure";
        }

        log.info("支付宝通知验签成功");
        
        // 获取关键参数
        String appId = params.get("app_id");
        String outTradeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");
        
        log.info("支付宝异步通知关键参数 - 订单号: {}, 支付宝交易号: {}, 交易状态: {}, 订单金额: {}", 
                 outTradeNo, tradeNo, tradeStatus, totalAmount);

        // 2. 验证appId
        if (!alipayConfig.getAppId().equals(appId)) {
            log.warn("收到的AppID与配置不匹配. 期望: {}, 实际: {}", alipayConfig.getAppId(), appId);
            return "failure";
        }

        // 3. 处理业务逻辑
        try {
            boolean success = alipayService.handleAlipayNotification(params);
            if (success) {
                log.info("订单 {} 支付成功处理完成", outTradeNo);
                return "success";
            } else {
                log.error("订单 {} 支付处理失败", outTradeNo);
                return "failure";
            }
        } catch (Exception e) {
            log.error("处理支付宝通知时发生异常, 订单号: " + outTradeNo, e);
            return "failure";
        }
    }

    /**
     * 支付宝同步返回接口
     * 
     * @param params 支付宝返回参数
     * @return 处理结果页面
     */
    @GetMapping("/return")
    @Operation(summary = "支付宝同步返回", description = "处理支付宝页面跳转回商户网站的请求")
    public ResponseEntity<Void> handleAlipayReturn(@RequestParam Map<String, String> params) {
        log.info("收到支付宝同步返回请求: {}", params);
        
        // 验签
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getAlipayPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType());
        } catch (AlipayApiException e) {
            log.error("支付宝同步返回验签失败", e);
            
            // 尝试从参数中获取前端URL
            String frontendUrl = "http://localhost:5173"; // 默认值
            String passbackParams = params.get("passback_params");
            if (passbackParams != null && !passbackParams.isEmpty()) {
                try {
                    frontendUrl = java.net.URLDecoder.decode(passbackParams, "UTF-8");
                } catch (Exception ex) {
                    // 解析失败，使用默认URL
                }
            }
            
            // 验签失败时重定向到支付失败页面
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(frontendUrl + "/payment-result?status=failed"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        
        if (!signVerified) {
            log.warn("支付宝同步返回验签未通过");
            
            // 尝试从参数中获取前端URL
            String frontendUrl = "http://localhost:5173"; // 默认值
            String passbackParams = params.get("passback_params");
            if (passbackParams != null && !passbackParams.isEmpty()) {
                try {
                    frontendUrl = java.net.URLDecoder.decode(passbackParams, "UTF-8");
                } catch (Exception ex) {
                    // 解析失败，使用默认URL
                }
            }
            
            // 验签失败时重定向到支付失败页面
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(frontendUrl + "/payment-result?status=failed"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        
        // 获取订单号
        String outTradeNo = params.get("out_trade_no");
        log.info("支付宝同步返回订单号: {}", outTradeNo);
        
        // 获取前端回调URL
        String frontendUrl = "http://localhost:5173"; // 默认值
        String passbackParams = params.get("passback_params");
        if (passbackParams != null && !passbackParams.isEmpty()) {
            try {
                frontendUrl = java.net.URLDecoder.decode(passbackParams, "UTF-8");
                log.info("从支付宝回传参数中获取前端URL: {}", frontendUrl);
            } catch (Exception e) {
                log.error("解析前端URL参数失败", e);
            }
        }
        
        if (outTradeNo != null) {
            // 查询订单状态
            Order order = orderMapper.selectByOrderNo(outTradeNo);
            log.info("订单查询结果: {}", order);
            
            if (order != null) {
                // 根据实际订单状态判断支付结果
                String paymentStatus;
                if (order.getStatus() == Order.OrderStatus.PAID) {
                    paymentStatus = "success";
                    log.info("订单 {} 已支付成功", outTradeNo);
                } else if (order.getStatus() == Order.OrderStatus.PENDING_PAYMENT) {
                    // 可能异步通知还未到达或处理，返回pending状态
                    paymentStatus = "pending";
                    log.info("订单 {} 仍处于待支付状态，可能支付已完成但异步通知尚未处理", outTradeNo);
                } else {
                    paymentStatus = "failed";
                    log.info("订单 {} 状态异常: {}", outTradeNo, order.getStatus());
                }
                
                // 重定向到前端支付结果页面
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create(
                    frontendUrl + "/payment-result?status=" + paymentStatus + "&orderNo=" + outTradeNo
                ));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }
        }
        
        // 如果订单号为空或未查到订单，重定向到错误页面
        log.warn("未能查询到有效订单信息: {}", outTradeNo);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(
            frontendUrl + "/payment-result?status=error&orderNo=" + outTradeNo
        ));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 查询订单支付状态
     *
     * @param orderNo 订单号
     * @return 订单支付状态
     */
    @GetMapping("/query/{orderNo}")
    @Operation(summary = "查询订单支付状态", description = "根据订单号查询支付状态，用于前端轮询")
    public ResponseEntity<?> queryOrderPaymentStatus(@PathVariable String orderNo) {
        log.info("查询订单支付状态: {}", orderNo);
        
        // 查询订单
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            log.warn("查询订单不存在: {}", orderNo);
            return ResponseEntity.notFound().build();
        }
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("orderNo", orderNo);
        response.put("status", order.getStatus().toString());
        response.put("isPaid", order.getStatus() == Order.OrderStatus.PAID);
        
        // 如果已支付，返回支付时间
        if (order.getStatus() == Order.OrderStatus.PAID && order.getPaymentTime() != null) {
            response.put("paymentTime", order.getPaymentTime().toString());
        }
        
        log.info("订单 {} 状态查询结果: {}", orderNo, response);
        return ResponseEntity.ok(response);
    }
} 