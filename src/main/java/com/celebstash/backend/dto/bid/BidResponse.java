package com.celebstash.backend.dto.bid;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private Long productId;
    private String productName;
    private String productDescription;
    private String productImageUrl;
    private BigDecimal initialBidPrice;
    private BigDecimal currentBidPrice;
    private Long currentBidderId;
    private String currentBidderName;
    private LocalDateTime bidStartTime;
    private LocalDateTime bidEndTime;
    private boolean isActive;
    private boolean isWinner;
    private String bidStatus; // "ACTIVE", "WON", "LOST", "EXPIRED"
}