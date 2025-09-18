package com.celebstash.backend.dto.story;

import com.celebstash.backend.dto.product.ProductResponse;
import com.celebstash.backend.model.enums.StoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userImageUrl;
    private StoryType type;
    private String mediaUrl;
    private String caption;
    private ProductResponse product;
    private Integer viewsCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isExpired;
    private boolean isViewed;
}