package com.celebstash.backend.service;

import com.celebstash.backend.model.redis.OtpData;
import com.celebstash.backend.model.redis.RateLimitData;
import com.celebstash.backend.repository.redis.OtpRepository;
import com.celebstash.backend.repository.redis.RateLimitRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final RateLimitRepository rateLimitRepository;
    private final JavaMailSender emailSender;

    @Value("${app.otp.expiration}")
    private long otpExpirationMs;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit.per-minute}")
    private int ratePerMinute;

    @Value("${app.otp.rate-limit.per-day}")
    private int ratePerDay;

    public boolean sendOtp(String identifier, OtpData.OtpType type, HttpServletRequest request) {
        return sendOtp(identifier, type, request, null, null);
    }

    public boolean sendOtp(String identifier, OtpData.OtpType type, HttpServletRequest request, String fullName, String password) {
        String clientIp = getClientIp(request);

        try {
            // Check rate limits
            if (isRateLimited(identifier, clientIp)) {
                log.warn("Rate limit exceeded for identifier: {} from IP: {}", identifier, clientIp);
                return false;
            }

            // Generate OTP
            String otp = generateOtp();

            try {
                // Store OTP in Redis
                OtpData otpData = OtpData.builder()
                        .id(identifier)
                        .otp(otp)
                        .type(type)
                        .attempts(0)
                        .createdAt(Instant.now())
                        .fullName(fullName)
                        .password(password)
                        .timeToLive(TimeUnit.MILLISECONDS.toSeconds(otpExpirationMs))
                        .build();

                otpRepository.save(otpData);

                // Update rate limits
                updateRateLimits(identifier, clientIp);
            } catch (Exception e) {
                // Log Redis error but continue with OTP sending
                log.error("Redis error during OTP generation: {}", e.getMessage());
                // For development/testing, log the OTP
                log.info("Generated OTP for {}: {}", identifier, otp);
            }

            // Send OTP via email or SMS
            boolean otpSent = false;
            if (isEmail(identifier)) {
                otpSent = sendEmailOtp(identifier, otp, type);
            } else {
                otpSent = sendSmsOtp(identifier, otp, type);
            }

            if (!otpSent) {
                log.error("Failed to send OTP to {}", identifier);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Unexpected error during OTP sending: {}", e.getMessage());
            return false;
        }
    }

    public boolean verifyOtp(String identifier, String otp, OtpData.OtpType type) {
        return verifyAndGetOtp(identifier, otp, type).isPresent();
    }

    public Optional<OtpData> verifyAndGetOtp(String identifier, String otp, OtpData.OtpType type) {
        try {
            Optional<OtpData> otpDataOpt = otpRepository.findById(identifier);

            if (otpDataOpt.isEmpty()) {
                log.warn("No OTP found for identifier: {}", identifier);
                return Optional.empty();
            }

            OtpData otpData = otpDataOpt.get();

            // Check if OTP type matches
            if (otpData.getType() != type) {
                log.warn("OTP type mismatch for identifier: {}", identifier);
                return Optional.empty();
            }

            // Check if OTP is expired
            if (otpData.isExpired()) {
                log.warn("OTP expired for identifier: {}", identifier);
                try {
                    otpRepository.delete(otpData);
                } catch (Exception e) {
                    log.error("Redis error during OTP deletion: {}", e.getMessage());
                }
                return Optional.empty();
            }

            // Check if max attempts exceeded
            if (otpData.hasExceededMaxAttempts(maxAttempts)) {
                log.warn("Max OTP attempts exceeded for identifier: {}", identifier);
                try {
                    otpRepository.delete(otpData);
                } catch (Exception e) {
                    log.error("Redis error during OTP deletion: {}", e.getMessage());
                }
                return Optional.empty();
            }

            // Increment attempts
            otpData.incrementAttempts();
            try {
                otpRepository.save(otpData);
            } catch (Exception e) {
                log.error("Redis error during OTP update: {}", e.getMessage());
            }

            // Verify OTP
            if (!otpData.getOtp().equals(otp)) {
                log.warn("Invalid OTP for identifier: {}", identifier);
                return Optional.empty();
            }

            // OTP verified, get a copy of the data before deleting it
            OtpData verifiedData = OtpData.builder()
                    .id(otpData.getId())
                    .otp(otpData.getOtp())
                    .type(otpData.getType())
                    .attempts(otpData.getAttempts())
                    .createdAt(otpData.getCreatedAt())
                    .fullName(otpData.getFullName())
                    .password(otpData.getPassword())
                    .timeToLive(otpData.getTimeToLive())
                    .build();

            // Delete the OTP from Redis
            try {
                otpRepository.delete(otpData);
            } catch (Exception e) {
                log.error("Redis error during OTP deletion: {}", e.getMessage());
            }

            return Optional.of(verifiedData);
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }

    private boolean isRateLimited(String identifier, String clientIp) {
        try {
            String minuteKey = RateLimitData.generateKey(identifier, clientIp, RateLimitData.LimitType.OTP_SEND);
            Optional<RateLimitData> rateLimitOpt = rateLimitRepository.findById(minuteKey);

            if (rateLimitOpt.isPresent()) {
                RateLimitData rateLimit = rateLimitOpt.get();

                // Check minute limit
                if (rateLimit.isMinuteLimitReached(ratePerMinute)) {
                    return true;
                }

                // Check day limit
                if (rateLimit.isDayLimitReached(ratePerDay)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            // Log Redis error but don't rate limit if Redis is unavailable
            log.error("Redis error during rate limit check: {}", e.getMessage());
            return false;
        }
    }

    private void updateRateLimits(String identifier, String clientIp) {
        try {
            String key = RateLimitData.generateKey(identifier, clientIp, RateLimitData.LimitType.OTP_SEND);
            Optional<RateLimitData> rateLimitOpt = rateLimitRepository.findById(key);

            RateLimitData rateLimit;
            if (rateLimitOpt.isPresent()) {
                rateLimit = rateLimitOpt.get();

                // Reset minute count if last request was more than a minute ago
                if (rateLimit.getLastRequest().plusSeconds(60).isBefore(Instant.now())) {
                    rateLimit.resetMinuteCount();
                }

                rateLimit.incrementMinuteCount();
                rateLimit.incrementDayCount();
                rateLimit.setLastRequest(Instant.now());
            } else {
                rateLimit = RateLimitData.builder()
                        .id(key)
                        .minuteCount(1)
                        .dayCount(1)
                        .lastRequest(Instant.now())
                        .timeToLive(TimeUnit.HOURS.toSeconds(24))
                        .build();
            }

            rateLimitRepository.save(rateLimit);
        } catch (Exception e) {
            // Log Redis error but continue
            log.error("Redis error during rate limit update: {}", e.getMessage());
        }
    }

    private boolean isEmail(String identifier) {
        return identifier.contains("@");
    }

    private boolean sendEmailOtp(String email, String otp, OtpData.OtpType type) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(getSubject(type));
            message.setText(getEmailBody(otp, type));
            emailSender.send(message);
            log.info("OTP email sent to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            return false;
        }
    }

    private boolean sendSmsOtp(String phoneNumber, String otp, OtpData.OtpType type) {
        try {
            // In a real implementation, this would use an SMS service provider
            log.info("OTP SMS would be sent to: {} with code: {}", phoneNumber, otp);
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP SMS: {}", e.getMessage());
            return false;
        }
    }

    private String getSubject(OtpData.OtpType type) {
        return switch (type) {
            case SIGNUP -> "Your Verification Code";
            case PASSWORD_RESET -> "Your Password Reset Code";
        };
    }

    private String getEmailBody(String otp, OtpData.OtpType type) {
        return switch (type) {
            case SIGNUP -> "Your verification code is: " + otp + ". It will expire in 10 minutes.";
            case PASSWORD_RESET -> "Your password reset code is: " + otp + ". It will expire in 10 minutes.";
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
