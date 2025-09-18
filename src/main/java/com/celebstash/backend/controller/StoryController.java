package com.celebstash.backend.controller;

import com.celebstash.backend.dto.story.StoryRequest;
import com.celebstash.backend.dto.story.StoryResponse;
import com.celebstash.backend.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Tag(name = "Stories", description = "Story management APIs")
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    @Operation(summary = "Create a new story", description = "Creates a new story with the provided details")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<StoryResponse> createStory(@Valid @RequestBody StoryRequest request) {
        return new ResponseEntity<>(storyService.createStory(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all active stories", description = "Returns all active (non-expired) stories")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<StoryResponse>> getActiveStories() {
        return ResponseEntity.ok(storyService.getActiveStories());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get active stories by user", description = "Returns all active stories by a specific user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<StoryResponse>> getActiveStoriesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(storyService.getActiveStoriesByUser(userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get story by ID", description = "Returns a story by its ID and marks it as viewed")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<StoryResponse> getStoryById(@PathVariable("id") Long storyId) {
        return ResponseEntity.ok(storyService.getStoryById(storyId));
    }

    @GetMapping("/unviewed")
    @Operation(summary = "Get unviewed stories", description = "Returns all active stories not viewed by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<StoryResponse>> getUnviewedStories() {
        return ResponseEntity.ok(storyService.getUnviewedStories());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete story", description = "Deletes a story (only the creator can delete their own stories)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteStory(@PathVariable("id") Long storyId) {
        storyService.deleteStory(storyId);
        return ResponseEntity.noContent().build();
    }
}