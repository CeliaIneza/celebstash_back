package com.celebstash.backend.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotBlank(message = "Video URL is required")
    private String videoUrl;

    @Size(min = 3, max = 5, message = "Between 3 and 5 photos are required")
    private List<String> photoUrls;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
}