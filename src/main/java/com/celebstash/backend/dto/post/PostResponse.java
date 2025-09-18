package com.celebstash.backend.dto.post;

import com.celebstash.backend.dto.comment.CommentResponse;
import com.celebstash.backend.dto.product.ProductResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userImageUrl;
    private ProductResponse product;
    private String videoUrl;
    private List<String> photoUrls;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Counts
    private int likesCount;
    private int commentsCount;
    private int sharesCount;
    
    // User interactions
    private boolean isLiked;
    private boolean isShared;
    
    // Comments (optional, may be loaded separately)
    private List<CommentResponse> comments;
}