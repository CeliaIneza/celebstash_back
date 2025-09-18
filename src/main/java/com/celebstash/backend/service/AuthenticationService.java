package com.celebstash.backend.service;

import com.celebstash.backend.dto.auth.*;
import com.celebstash.backend.model.RefreshToken;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.AccountStatus;
import com.celebstash.backend.model.redis.OtpData;
import com.celebstash.backend.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final OtpService otpService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public boolean initiateSignup(SignupRequest request, HttpServletRequest httpRequest) {
        // Validate password match
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            log.warn("Password mismatch during signup for identifier: {}", request.getIdentifier());
            return false;
        }

        boolean isEmail = isEmail(request.getIdentifier());

        // Check if user already exists
        if (isEmail && userService.existsByEmail(request.getIdentifier())) {
            log.warn("Email already exists: {}", request.getIdentifier());
            return false;
        } else if (!isEmail && userService.existsByPhoneNumber(request.getIdentifier())) {
            log.warn("Phone number already exists: {}", request.getIdentifier());
            return false;
        }

        // Send OTP with user information
        return otpService.sendOtp(
            request.getIdentifier(), 
            OtpData.OtpType.SIGNUP, 
            httpRequest,
            request.getFullName(),
            request.getPassword()
        );
    }

    @Transactional
    public AuthResponse completeSignup(OtpVerificationRequest request, HttpServletRequest httpRequest) {
        // Verify OTP and get user information
        Optional<OtpData> otpDataOpt = otpService.verifyAndGetOtp(request.getIdentifier(), request.getOtp(), OtpData.OtpType.SIGNUP);
        if (otpDataOpt.isEmpty()) {
            log.warn("Invalid OTP during signup for identifier: {}", request.getIdentifier());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid or expired OTP")
                    .build();
        }

        // Get user information from OTP data
        OtpData otpData = otpDataOpt.get();
        String fullName = otpData.getFullName();
        String password = otpData.getPassword();

        if (fullName == null || password == null) {
            log.error("Missing user information for signup: {}", request.getIdentifier());
            return AuthResponse.builder()
                    .success(false)
                    .message("Missing user information. Please try signing up again.")
                    .build();
        }

        // Get user from temporary storage or create new user
        boolean isEmail = isEmail(request.getIdentifier());

        // Create user
        User user = userService.createUser(
                fullName,
                request.getIdentifier(),
                password,
                isEmail
        );

        // Verify user
        user = userService.verifyUser(user, isEmail);

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);

        // Build response
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(AuthRequest request, HttpServletRequest httpRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();

            // Check if user is verified
            if (user.getStatus() == AccountStatus.PENDING) {
                return AuthResponse.builder()
                        .success(false)
                        .message("Account not verified")
                        .build();
            }

            // Generate tokens
            String accessToken = jwtUtils.generateAccessToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);

            // Build response
            return buildAuthResponse(user, accessToken, refreshToken.getToken());
        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid credentials")
                    .build();
        }
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenService.validateRefreshToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Invalid or expired refresh token")
                    .build();
        }

        RefreshToken token = tokenOpt.get();
        User user = token.getUser();

        // Rotate refresh token
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(token, httpRequest);

        // Generate new access token
        String accessToken = jwtUtils.generateAccessToken(user);

        // Build response
        return buildAuthResponse(user, accessToken, newRefreshToken.getToken());
    }

    @Transactional
    public boolean logout(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        Optional<RefreshToken> tokenOpt = refreshTokenService.validateRefreshToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        // Revoke refresh token
        refreshTokenService.revokeRefreshToken(tokenOpt.get());
        return true;
    }

    @Transactional
    public boolean initiatePasswordReset(String identifier, HttpServletRequest httpRequest) {
        // Check if user exists
        Optional<User> userOpt = userService.findByEmailOrPhoneNumber(identifier);
        if (userOpt.isEmpty()) {
            // For security, don't reveal if user exists
            log.info("Password reset requested for non-existent user: {}", identifier);
            return true;
        }

        // Send OTP
        return otpService.sendOtp(identifier, OtpData.OtpType.PASSWORD_RESET, httpRequest);
    }

    @Transactional
    public boolean completePasswordReset(PasswordResetRequest request) {
        // Validate password match
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            log.warn("Password mismatch during reset for identifier: {}", request.getIdentifier());
            return false;
        }

        // Verify OTP
        if (!otpService.verifyOtp(request.getIdentifier(), request.getOtp(), OtpData.OtpType.PASSWORD_RESET)) {
            log.warn("Invalid OTP during password reset for identifier: {}", request.getIdentifier());
            return false;
        }

        // Find user
        Optional<User> userOpt = userService.findByEmailOrPhoneNumber(request.getIdentifier());
        if (userOpt.isEmpty()) {
            log.warn("User not found during password reset: {}", request.getIdentifier());
            return false;
        }

        User user = userOpt.get();

        // Update password
        userService.updatePassword(user, request.getNewPassword());

        // Revoke all refresh tokens
        refreshTokenService.revokeAllUserTokens(user);

        return true;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.extractExpiration(accessToken).getTime() - System.currentTimeMillis())
                .userId(user.getId().toString())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .success(true)
                .build();
    }

    private boolean isEmail(String identifier) {
        return identifier.contains("@");
    }
}
