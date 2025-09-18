package com.celebstash.backend.dto.like;

import com.celebstash.backend.model.enums.LikeableType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userImageUrl;
    private LikeableType likeableType;
    private Long likeableId;
    private LocalDateTime createdAt;
}