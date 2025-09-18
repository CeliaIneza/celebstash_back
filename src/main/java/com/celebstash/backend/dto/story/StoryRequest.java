package com.celebstash.backend.dto.story;

import com.celebstash.backend.model.enums.StoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryRequest {

    @NotNull(message = "Story type is required")
    private StoryType type;

    @NotBlank(message = "Media URL is required")
    private String mediaUrl;

    @Size(max = 500, message = "Caption cannot exceed 500 characters")
    private String caption;

    // Optional product ID to link the story to a product
    private Long productId;

    // Optional expiration time (defaults to 24 hours from creation if not provided)
    private LocalDateTime expiresAt;
}