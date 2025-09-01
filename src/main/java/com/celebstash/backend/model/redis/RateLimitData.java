package com.celebstash.backend.model.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("rate_limit")
public class RateLimitData implements Serializable {

    @Id
    private String id; // identifier:ip:action (e.g., email@example.com:192.168.1.1:otp_send)

    private int minuteCount;
    private int dayCount;
    private Instant lastRequest;

    @TimeToLive
    private Long timeToLive; // in seconds, 24 hours for daily limits

    public enum LimitType {
        OTP_SEND,
        LOGIN_ATTEMPT
    }

    public boolean isMinuteLimitReached(int limit) {
        return minuteCount >= limit;
    }

    public boolean isDayLimitReached(int limit) {
        return dayCount >= limit;
    }

    public void incrementMinuteCount() {
        this.minuteCount++;
    }

    public void incrementDayCount() {
        this.dayCount++;
    }

    public void resetMinuteCount() {
        this.minuteCount = 0;
    }

    public static String generateKey(String identifier, String ipAddress, LimitType type) {
        return String.format("%s:%s:%s", identifier, ipAddress, type.name());
    }
}