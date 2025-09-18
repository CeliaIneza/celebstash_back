package com.celebstash.backend.dto.share;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequest {

    @NotNull(message = "Post ID is required")
    private Long postId;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;
}