package com.celebstash.backend.model.enums;

public enum AccountStatus {
    PENDING,    // Initial state, waiting for verification
    VERIFIED,   // Email/phone verified but not fully active
    ACTIVE,     // Fully active account
    LOCKED,     // Account locked due to suspicious activity or too many failed attempts
    DISABLED    // Account disabled by admin or user
}