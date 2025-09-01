package com.celebstash.backend.controller;

import com.celebstash.backend.dto.auth.*;
import com.celebstash.backend.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/signup/initiate")
    @Operation(summary = "Initiate signup process", description = "Validates signup data and sends OTP to email or phone")
    public ResponseEntity<ApiResponse> initiateSignup(@Valid @RequestBody SignupRequest request, HttpServletRequest httpRequest) {
        boolean success = authenticationService.initiateSignup(request, httpRequest);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent successfully"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to initiate signup"));
        }
    }

    @PostMapping("/signup/verify")
    @Operation(summary = "Complete signup with OTP", description = "Verifies OTP and completes user registration")
    public ResponseEntity<AuthResponse> completeSignup(@Valid @RequestBody OtpVerificationRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.completeSignup(request, httpRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticates user with email/phone and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.login(request, httpRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh token", description = "Issues new access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        AuthResponse response = authenticationService.refreshToken(request, httpRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the refresh token")
    public ResponseEntity<ApiResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        boolean success = authenticationService.logout(request);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Invalid token"));
        }
    }

    @PostMapping("/password-reset/initiate")
    @Operation(summary = "Initiate password reset", description = "Sends OTP to email or phone for password reset")
    public ResponseEntity<ApiResponse> initiatePasswordReset(@RequestParam String identifier, HttpServletRequest httpRequest) {
        authenticationService.initiatePasswordReset(identifier, httpRequest);
        // Always return success for security (don't reveal if user exists)
        return ResponseEntity.ok(new ApiResponse(true, "If an account exists, a password reset OTP has been sent"));
    }

    @PostMapping("/password-reset/complete")
    @Operation(summary = "Complete password reset", description = "Verifies OTP and updates password")
    public ResponseEntity<ApiResponse> completePasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        boolean success = authenticationService.completePasswordReset(request);
        if (success) {
            return ResponseEntity.ok(new ApiResponse(true, "Password reset successfully"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Failed to reset password"));
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    public static class ApiResponse {
        private final boolean success;
        private final String message;

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
