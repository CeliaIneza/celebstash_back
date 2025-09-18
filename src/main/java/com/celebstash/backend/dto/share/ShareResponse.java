package com.celebstash.backend.dto.share;

import com.celebstash.backend.dto.post.PostResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userImageUrl;
    private PostResponse post;
    private String message;
    private LocalDateTime createdAt;
}