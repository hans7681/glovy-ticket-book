package com.backend.backend.tasks;

import com.backend.backend.service.SeatLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class ScheduledTasks {

    @Autowired
    private SeatLockService seatLockService;

    // 定义清理任务的执行频率 (Cron表达式)
    // 例如: 每5分钟执行一次 "0 */5 * * * ?"
    // 例如: 每分钟执行一次 "0 * * * * ?"
    // 这里设置为每分钟执行一次，方便测试，生产环境可适当调整
    private static final String CLEANUP_CRON_EXPRESSION = "0 * * * * ?"; 

    @Scheduled(cron = CLEANUP_CRON_EXPRESSION)
    public void cleanupExpiredSeatLocks() {
        log.info("Running scheduled task: Cleaning up expired seat locks older than {}", LocalDateTime.now());
        try {
            int deletedCount = seatLockService.deleteExpiredLocks(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("Scheduled task finished: Deleted {} expired seat locks.", deletedCount);
            } else {
                log.info("Scheduled task finished: No expired seat locks found to delete.");
            }
        } catch (Exception e) {
            log.error("Error during scheduled cleanup of expired seat locks: {}", e.getMessage(), e);
        }
    }
} 