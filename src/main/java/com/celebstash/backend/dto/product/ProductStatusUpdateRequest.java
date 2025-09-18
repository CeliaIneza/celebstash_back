package com.celebstash.backend.dto.product;

import com.celebstash.backend.model.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatusUpdateRequest {

    @NotNull(message = "Product status is required")
    private ProductStatus status;
    
    private String rejectionReason;
}