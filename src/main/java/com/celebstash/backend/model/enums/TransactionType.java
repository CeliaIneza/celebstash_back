package com.celebstash.backend.model.enums;

/**
 * Enum representing the type of transaction
 */
public enum TransactionType {
    DEPOSIT,    // Adding money to wallet
    PURCHASE,   // Buying a product
    BID,        // Placing a bid
    BID_REFUND  // Refund for outbid or unsuccessful bid
}