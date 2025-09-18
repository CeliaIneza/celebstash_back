package com.celebstash.backend.model.enums;

public enum ProductStatus {
    PENDING,    // Initial state, waiting for admin approval
    APPROVED,   // Approved by admin, visible to all users
    REJECTED    // Rejected by admin, not visible to users
}