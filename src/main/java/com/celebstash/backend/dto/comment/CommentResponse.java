package com.celebstash.backend.dto.comment;

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
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long userId;
    private String userName;
    private String userImageUrl;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Parent comment info (if this is a reply)
    private Long parentId;
    private boolean isReply;
    
    // Counts
    private int likesCount;
    private int repliesCount;
    
    // User interactions
    private boolean isLiked;
    
    // Replies (optional, may be loaded separately)
    private List<CommentResponse> replies;
}