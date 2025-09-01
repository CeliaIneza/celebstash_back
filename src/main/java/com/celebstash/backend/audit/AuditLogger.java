package com.celebstash.backend.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
public class AuditLogger {

    private static final String AUDIT_LOGGER = "AUDIT";

    public void logSignupInitiated(String identifier, HttpServletRequest request) {
        logAuthEvent("SIGNUP_INITIATED", identifier, getClientInfo(request));
    }

    public void logSignupCompleted(String identifier, HttpServletRequest request) {
        logAuthEvent("SIGNUP_COMPLETED", identifier, getClientInfo(request));
    }

    public void logSignupFailed(String identifier, String reason, HttpServletRequest request) {
        logAuthEvent("SIGNUP_FAILED", identifier, reason, getClientInfo(request));
    }

    public void logLoginSuccess(String identifier, HttpServletRequest request) {
        logAuthEvent("LOGIN_SUCCESS", identifier, getClientInfo(request));
    }

    public void logLoginFailed(String identifier, String reason, HttpServletRequest request) {
        logAuthEvent("LOGIN_FAILED", identifier, reason, getClientInfo(request));
    }

    public void logLogout(String identifier, HttpServletRequest request) {
        logAuthEvent("LOGOUT", identifier, getClientInfo(request));
    }

    public void logTokenRefresh(String identifier, HttpServletRequest request) {
        logAuthEvent("TOKEN_REFRESH", identifier, getClientInfo(request));
    }

    public void logPasswordResetInitiated(String identifier, HttpServletRequest request) {
        logAuthEvent("PASSWORD_RESET_INITIATED", identifier, getClientInfo(request));
    }

    public void logPasswordResetCompleted(String identifier, HttpServletRequest request) {
        logAuthEvent("PASSWORD_RESET_COMPLETED", identifier, getClientInfo(request));
    }

    public void logPasswordResetFailed(String identifier, String reason, HttpServletRequest request) {
        logAuthEvent("PASSWORD_RESET_FAILED", identifier, reason, getClientInfo(request));
    }

    public void logOtpGenerated(String identifier, HttpServletRequest request) {
        logAuthEvent("OTP_GENERATED", identifier, getClientInfo(request));
    }

    public void logOtpVerified(String identifier, HttpServletRequest request) {
        logAuthEvent("OTP_VERIFIED", identifier, getClientInfo(request));
    }

    public void logOtpVerificationFailed(String identifier, String reason, HttpServletRequest request) {
        logAuthEvent("OTP_VERIFICATION_FAILED", identifier, reason, getClientInfo(request));
    }

    private void logAuthEvent(String event, String identifier, String clientInfo) {
        log.info("{} | {} | {} | {} | {}", 
                AUDIT_LOGGER, 
                generateEventId(), 
                event, 
                maskIdentifier(identifier), 
                clientInfo);
    }

    private void logAuthEvent(String event, String identifier, String reason, String clientInfo) {
        log.info("{} | {} | {} | {} | {} | {}", 
                AUDIT_LOGGER, 
                generateEventId(), 
                event, 
                maskIdentifier(identifier), 
                reason,
                clientInfo);
    }

    private String generateEventId() {
        return UUID.randomUUID().toString();
    }

    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "unknown";
        }
        
        // Mask email
        if (identifier.contains("@")) {
            String[] parts = identifier.split("@");
            if (parts[0].length() > 2) {
                return parts[0].substring(0, 2) + "***@" + parts[1];
            } else {
                return "***@" + parts[1];
            }
        }
        
        // Mask phone number
        if (identifier.length() > 4) {
            return "***" + identifier.substring(identifier.length() - 4);
        }
        
        return "***";
    }

    private String getClientInfo(HttpServletRequest request) {
        if (request == null) {
            return "ip=unknown,agent=unknown";
        }
        
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        userAgent = userAgent != null ? userAgent : "unknown";
        
        return String.format("ip=%s,agent=%s,time=%s", ip, userAgent, Instant.now());
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}