package com.celebstash.backend.dto.bid;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Bid amount is required")
    @Min(value = 0, message = "Bid amount must be greater than or equal to 0")
    private BigDecimal bidAmount;
}