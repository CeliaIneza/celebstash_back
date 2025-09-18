package com.celebstash.backend.model.enums;

/**
 * Enum representing the status of a transaction
 */
public enum TransactionStatus {
    PENDING,    // Transaction is in progress
    COMPLETED,  // Transaction has been completed successfully
    FAILED,     // Transaction has failed
    REFUNDED    // Transaction has been refunded
}