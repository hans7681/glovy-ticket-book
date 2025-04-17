package com.backend.backend.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Generates a unique order number.
     * Format: yyyyMMddHHmmssSSS (17 digits) + 3 digit random number + 3 digit sequence number
     * Total length: 23 digits
     * This aims for high probability of uniqueness, especially under moderate load.
     * Consider database sequence or UUID for absolute guarantee if needed.
     *
     * @return A generated order number string.
     */
    public String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DATE_TIME_FORMATTER);

        // Get a 3-digit random number (000-999)
        int randomNum = ThreadLocalRandom.current().nextInt(1000);
        String randomStr = String.format("%03d", randomNum);

        // Get a 3-digit sequence number (000-999), cycling
        int seqNum = sequence.incrementAndGet() % 1000;
        String sequenceStr = String.format("%03d", seqNum);

        return timestamp + randomStr + sequenceStr;
    }
} 