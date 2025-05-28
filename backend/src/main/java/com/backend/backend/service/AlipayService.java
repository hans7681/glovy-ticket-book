package com.backend.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.backend.backend.config.AlipayConfig;
import com.backend.backend.entity.Order;
import com.backend.backend.mapper.OrderMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝支付服务类
 */
@Service
@Slf4j
public class AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 创建支付宝支付订单
     *
     * @param orderNo 订单号
     * @param frontendUrl 前端回调URL
     * @return 支付表单HTML
     */
    public String createPayment(String orderNo, String frontendUrl) throws Exception {
        // 获取订单信息
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 校验订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("订单状态不正确，无法支付");
        }
        
        // 保存前端回调URL
        order.setFrontendCallbackUrl(frontendUrl);
        orderMapper.updateById(order);

        // 创建支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        // 设置同步返回地址
        request.setReturnUrl(alipayConfig.getReturnUrl());
        // 设置异步通知地址
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        // 构建业务参数
        String bizContent = "{" +
                "\"out_trade_no\":\"" + orderNo + "\"," +
                "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
                "\"total_amount\":" + order.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP) + "," +
                "\"subject\":\"电影票订单 - " + orderNo + "\"," +
                "\"passback_params\":\"" + java.net.URLEncoder.encode(frontendUrl, "UTF-8") + "\"" +
                "}";
        request.setBizContent(bizContent);

        try {
            // 调用支付宝接口获取支付表单HTML
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("创建支付宝支付订单失败", e);
            throw new RuntimeException("支付宝支付失败：" + e.getMessage());
        }
    }

    /**
     * 处理支付宝异步通知
     *
     * @param params 支付宝通知参数
     * @return 处理结果
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayNotification(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String totalAmount = params.get("total_amount");
        String tradeNo = params.get("trade_no"); // 支付宝交易号
        
        log.info("===== 支付宝异步通知开始处理 =====");
        log.info("订单号: {}, 支付宝交易号: {}, 交易状态: {}, 金额: {}", 
                 outTradeNo, tradeNo, tradeStatus, totalAmount);
        
        try {
            // 查询订单
            Order order = orderMapper.selectByOrderNo(outTradeNo);
            if (order == null) {
                log.warn("订单不存在: {}", outTradeNo);
                return false;
            }
            
            // 校验金额
            BigDecimal expectedAmount = order.getTotalAmount();
            BigDecimal actualAmount = new BigDecimal(totalAmount);
            if (expectedAmount.compareTo(actualAmount) != 0) {
                log.warn("订单金额不匹配. 期望: {}, 实际: {}", expectedAmount, actualAmount);
                return false;
            }
            
            // 校验订单状态，防止重复处理
            if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
                log.info("订单 {} 已处理. 状态: {}, 无需重复更新", outTradeNo, order.getStatus());
                return true; // 已处理，视为成功
            }
            
            // 处理支付成功状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                log.info("处理订单 {} 的支付成功通知，支付宝交易号: {}", outTradeNo, tradeNo);
                
                // 更新订单状态
                order.setStatus(Order.OrderStatus.PAID);
                order.setPaymentTime(LocalDateTime.now());
                // 存储支付宝交易号，如果Order类中有此字段
                try {
                    order.setAlipayTradeNo(tradeNo);
                } catch (Exception e) {
                    // 如果字段不存在，忽略此错误
                    log.warn("无法设置支付宝交易号，可能字段不存在", e);
                }
                
                int updated = orderMapper.updateById(order);
                
                if (updated > 0) {
                    log.info("订单 {} 状态已成功更新为已支付", outTradeNo);
                    log.info("===== 支付宝异步通知处理完成: 成功 =====");
                    return true;
                } else {
                    log.error("更新订单 {} 状态失败", outTradeNo);
                    log.info("===== 支付宝异步通知处理完成: 失败 =====");
                    return false;
                }
            } else {
                log.info("订单 {} 收到非成功交易状态: {}, 不更新订单状态", outTradeNo, tradeStatus);
                log.info("===== 支付宝异步通知处理完成: 不处理 =====");
                return true; // 非成功状态，无需处理
            }
        } catch (Exception e) {
            log.error("===== 支付宝异步通知处理异常 =====", e);
            // 事务会回滚
            throw e;
        }
    }
} 