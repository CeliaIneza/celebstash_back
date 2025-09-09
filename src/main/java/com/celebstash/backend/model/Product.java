package com.celebstash.backend.model;

import com.celebstash.backend.model.enums.ProductStatus;
import com.celebstash.backend.model.enums.ProductType;
import jakarta.persistence.*;
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
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String imageUrl;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType productType = ProductType.REGULAR;

    @Column
    private BigDecimal initialBidPrice;

    @Column
    private BigDecimal currentBidPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bidder_id")
    private User currentBidder;

    @Column
    private LocalDateTime bidStartTime;

    @Column
    private LocalDateTime bidEndTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ProductStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
