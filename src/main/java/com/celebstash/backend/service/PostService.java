package com.celebstash.backend.service;

import com.celebstash.backend.dto.post.PostRequest;
import com.celebstash.backend.dto.post.PostResponse;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Post;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.LikeableType;
import com.celebstash.backend.repository.LikeRepository;
import com.celebstash.backend.repository.PostRepository;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.ShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;
    private final ShareRepository shareRepository;
    private final UserService userService;

    /**
     * Create a new post
     * @param request the post request
     * @return the created post response
     */
    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = userService.getCurrentUser();
        
        // Validate photo count
        if (request.getPhotoUrls() == null || request.getPhotoUrls().size() < 3 || request.getPhotoUrls().size() > 5) {
            throw new AppException("Between 3 and 5 photos are required", HttpStatus.BAD_REQUEST);
        }
        
        // Get the product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        
        // Only the product owner can create a post for it
        if (!product.getSeller().getId().equals(currentUser.getId())) {
            throw new AppException("You can only create posts for your own products", HttpStatus.FORBIDDEN);
        }
        
        // Create the post
        Post post = Post.builder()
                .user(currentUser)
                .product(product)
                .videoUrl(request.getVideoUrl())
                .photoUrls(request.getPhotoUrls())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .build();
        
        Post savedPost = postRepository.save(post);
        
        return mapToPostResponse(savedPost, currentUser);
    }

    /**
     * Get all posts
     * @param pageable pagination information
     * @return page of post responses
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<Post> posts = postRepository.findAll(pageable);
        
        return posts.map(post -> mapToPostResponse(post, currentUser));
    }

    /**
     * Get a post by ID
     * @param postId the post ID
     * @return the post response
     */
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId) {
        User currentUser = userService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post not found", HttpStatus.NOT_FOUND));
        
        return mapToPostResponse(post, currentUser);
    }

    /**
     * Get all posts by the current user
     * @param pageable pagination information
     * @return page of post responses
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getMyPosts(Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        Page<Post> posts = postRepository.findByUser(currentUser, pageable);
        
        return posts.map(post -> mapToPostResponse(post, currentUser));
    }

    /**
     * Update a post
     * @param postId the post ID
     * @param request the post request
     * @return the updated post response
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = userService.getCurrentUser();
        
        // Validate photo count
        if (request.getPhotoUrls() == null || request.getPhotoUrls().size() < 3 || request.getPhotoUrls().size() > 5) {
            throw new AppException("Between 3 and 5 photos are required", HttpStatus.BAD_REQUEST);
        }
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post not found", HttpStatus.NOT_FOUND));
        
        // Only the post owner can update it
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only update your own posts", HttpStatus.FORBIDDEN);
        }
        
        // Update the post
        post.setVideoUrl(request.getVideoUrl());
        post.setPhotoUrls(request.getPhotoUrls());
        post.setDescription(request.getDescription());
        post.setUpdatedAt(LocalDateTime.now());
        
        Post updatedPost = postRepository.save(post);
        
        return mapToPostResponse(updatedPost, currentUser);
    }

    /**
     * Delete a post
     * @param postId the post ID
     */
    @Transactional
    public void deletePost(Long postId) {
        User currentUser = userService.getCurrentUser();
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post not found", HttpStatus.NOT_FOUND));
        
        // Only the post owner can delete it
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new AppException("You can only delete your own posts", HttpStatus.FORBIDDEN);
        }
        
        postRepository.delete(post);
    }

    /**
     * Like a post
     * @param postId the post ID
     * @return the updated post response
     */
    @Transactional
    public PostResponse likePost(Long postId) {
        User currentUser = userService.getCurrentUser();
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post not found", HttpStatus.NOT_FOUND));
        
        // Check if the user has already liked the post
        if (post.isLikedBy(currentUser)) {
            throw new AppException("You have already liked this post", HttpStatus.BAD_REQUEST);
        }
        
        // Add the like
        post.addLike(currentUser);
        Post updatedPost = postRepository.save(post);
        
        return mapToPostResponse(updatedPost, currentUser);
    }

    /**
     * Unlike a post
     * @param postId the post ID
     * @return the updated post response
     */
    @Transactional
    public PostResponse unlikePost(Long postId) {
        User currentUser = userService.getCurrentUser();
        
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post not found", HttpStatus.NOT_FOUND));
        
        // Check if the user has liked the post
        if (!post.isLikedBy(currentUser)) {
            throw new AppException("You have not liked this post", HttpStatus.BAD_REQUEST);
        }
        
        // Remove the like
        post.removeLike(currentUser);
        Post updatedPost = postRepository.save(post);
        
        return mapToPostResponse(updatedPost, currentUser);
    }

    /**
     * Map a Post entity to a PostResponse DTO
     * @param post the post entity
     * @param currentUser the current user
     * @return the post response DTO
     */
    private PostResponse mapToPostResponse(Post post, User currentUser) {
        return PostResponse.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .userName(post.getUser().getFullName())
                .userImageUrl(null) // TODO: Add user image URL when available
                .product(null) // TODO: Add product response when available
                .videoUrl(post.getVideoUrl())
                .photoUrls(post.getPhotoUrls())
                .description(post.getDescription())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .isLiked(post.isLikedBy(currentUser))
                .isShared(false) // TODO: Implement check if user has shared the post
                .comments(null) // Comments are loaded separately
                .build();
    }
}