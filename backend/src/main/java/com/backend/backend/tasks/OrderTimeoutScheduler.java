package com.backend.backend.tasks;

import com.backend.backend.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrderService orderService; 

    @Autowired
    public OrderTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    // 定义超时时间为 15 分钟
    private static final int ORDER_TIMEOUT_MINUTES = 15;

    // 每分钟执行一次 (cron = "0 * * * * ?")
    @Scheduled(cron = "0 * * * * ?") 
    public void cancelExpiredPendingOrdersTask() {
        log.info("开始执行定时任务：取消超时未支付订单...");
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(ORDER_TIMEOUT_MINUTES);

        try {
            // 调用 Service 层的方法来执行取消逻辑
            int cancelledCount = orderService.cancelTimedOutPendingOrdersAndReleaseSeats(cutoffTime);
            if (cancelledCount > 0) {
                log.info("成功取消 {} 个超时未支付订单。", cancelledCount);
            } else {
                log.info("没有需要取消的超时未支付订单。", cutoffTime);
            }
        } catch (Exception e) {
            // 务必捕获异常，防止定时任务因单次错误而停止
            log.error("执行取消超时订单任务时发生错误:", e);
        }
    }
} 