package com.celebstash.backend.service;

import com.celebstash.backend.dto.product.ProductResponse;
import com.celebstash.backend.dto.story.StoryRequest;
import com.celebstash.backend.dto.story.StoryResponse;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.Story;
import com.celebstash.backend.model.User;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.StoryRepository;
import com.celebstash.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ProductService productService;

    /**
     * Create a new story
     * @param request the story request
     * @return the created story response
     */
    @Transactional
    public StoryResponse createStory(StoryRequest request) {
        User currentUser = userService.getCurrentUser();

        Story.StoryBuilder storyBuilder = Story.builder()
                .user(currentUser)
                .type(request.getType())
                .mediaUrl(request.getMediaUrl())
                .caption(request.getCaption())
                .createdAt(LocalDateTime.now());

        // Set expiration time (default is 24 hours from creation)
        if (request.getExpiresAt() != null) {
            storyBuilder.expiresAt(request.getExpiresAt());
        } else {
            storyBuilder.expiresAt(LocalDateTime.now().plusHours(24));
        }

        // Link to product if provided
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
            storyBuilder.product(product);
        }

        Story story = storyBuilder.build();
        Story savedStory = storyRepository.save(story);

        return mapToStoryResponse(savedStory, currentUser);
    }

    /**
     * Get all active stories
     * @return list of active story responses
     */
    @Transactional(readOnly = true)
    public List<StoryResponse> getActiveStories() {
        User currentUser = userService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<Story> activeStories = storyRepository.findActiveStories(now);

        return activeStories.stream()
                .map(story -> mapToStoryResponse(story, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Get all active stories by a specific user
     * @param userId the user ID
     * @return list of active story responses
     */
    @Transactional(readOnly = true)
    public List<StoryResponse> getActiveStoriesByUser(Long userId) {
        User currentUser = userService.getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        List<Story> activeStories = storyRepository.findActiveStoriesByUser(targetUser, now);

        return activeStories.stream()
                .map(story -> mapToStoryResponse(story, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Get a specific story by ID
     * @param storyId the story ID
     * @return the story response
     */
    @Transactional
    public StoryResponse getStoryById(Long storyId) {
        User currentUser = userService.getCurrentUser();

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException("Story not found", HttpStatus.NOT_FOUND));

        // Mark story as viewed by current user
        if (!story.isViewedBy(currentUser)) {
            story.addView(currentUser);
            storyRepository.save(story);
        }

        return mapToStoryResponse(story, currentUser);
    }

    /**
     * Get all stories not viewed by the current user
     * @return list of unviewed story responses
     */
    @Transactional(readOnly = true)
    public List<StoryResponse> getUnviewedStories() {
        User currentUser = userService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        List<Story> unviewedStories = storyRepository.findStoriesNotViewedBy(currentUser, now);

        return unviewedStories.stream()
                .map(story -> mapToStoryResponse(story, currentUser))
                .collect(Collectors.toList());
    }

    /**
     * Delete a story
     * @param storyId the story ID
     */
    @Transactional
    public void deleteStory(Long storyId) {
        User currentUser = userService.getCurrentUser();

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException("Story not found", HttpStatus.NOT_FOUND));

        // Only the creator can delete their story
        if (!story.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only delete your own stories", HttpStatus.FORBIDDEN);
        }

        storyRepository.delete(story);
    }

    /**
     * Map a Story entity to a StoryResponse DTO
     * @param story the story entity
     * @param currentUser the current user
     * @return the story response DTO
     */
    private StoryResponse mapToStoryResponse(Story story, User currentUser) {
        StoryResponse.StoryResponseBuilder builder = StoryResponse.builder()
                .id(story.getId())
                .userId(story.getUser().getId())
                .userName(story.getUser().getFullName())
                .userImageUrl(null) // TODO: Add user image URL when available
                .type(story.getType())
                .mediaUrl(story.getMediaUrl())
                .caption(story.getCaption())
                .viewsCount(story.getViewsCount())
                .createdAt(story.getCreatedAt())
                .expiresAt(story.getExpiresAt())
                .isExpired(story.isExpired())
                .isViewed(story.isViewedBy(currentUser));

        // Add product information if available
        if (story.getProduct() != null) {
            ProductResponse productResponse = productService.getProductById(story.getProduct().getId());
            builder.product(productResponse);
        }

        return builder.build();
    }
}
