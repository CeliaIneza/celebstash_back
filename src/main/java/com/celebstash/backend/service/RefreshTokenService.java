package com.celebstash.backend.service;

import com.celebstash.backend.model.RefreshToken;
import com.celebstash.backend.model.User;
import com.celebstash.backend.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        String tokenValue = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(tokenValue);
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(hashedToken)
                .expiryDate(Instant.now().plusMillis(refreshTokenExpiration))
                .issuedAt(Instant.now())
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .revoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        // Return the token with the plain value for the response
        refreshToken.setToken(tokenValue);
        return refreshToken;
    }

    @Transactional
    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findAll().stream()
                .filter(storedToken -> !storedToken.isRevoked() && !storedToken.isExpired() 
                        && passwordEncoder.matches(token, storedToken.getToken()))
                .findFirst();
    }

    @Transactional
    public void revokeRefreshToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.info("Refresh token revoked: {}", token.getId());
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("All refresh tokens revoked for user: {}", user.getUsername());
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, HttpServletRequest request) {
        // Revoke the old token
        revokeRefreshToken(oldToken);
        
        // Create a new token
        return createRefreshToken(oldToken.getUser(), request);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}