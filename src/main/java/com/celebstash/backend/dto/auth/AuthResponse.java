package com.celebstash.backend.dto.auth;

import com.celebstash.backend.model.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private AccountStatus status;
    private boolean emailVerified;
    private boolean phoneVerified;
    
    @Builder.Default
    private boolean success = true;
    
    private String message;
}