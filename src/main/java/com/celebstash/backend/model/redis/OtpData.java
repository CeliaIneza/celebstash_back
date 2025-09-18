package com.celebstash.backend.model.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("otp")
public class OtpData implements Serializable {

    @Id
    private String id; // email or phone number

    @Indexed
    private String otp;

    private OtpType type;

    private int attempts;

    private Instant createdAt;

    // User information for signup
    private String fullName;
    private String password;

    @TimeToLive
    private Long timeToLive; // in seconds

    public enum OtpType {
        SIGNUP,
        PASSWORD_RESET
    }

    public boolean isExpired() {
        return timeToLive <= 0;
    }

    public boolean hasExceededMaxAttempts(int maxAttempts) {
        return attempts >= maxAttempts;
    }

    public void incrementAttempts() {
        this.attempts++;
    }
}
