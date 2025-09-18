package com.celebstash.backend.repository;

import com.celebstash.backend.model.Post;
import com.celebstash.backend.model.Share;
import com.celebstash.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    // Find all shares by user
    List<Share> findByUser(User user);
    
    // Find all shares by user with pagination
    Page<Share> findByUser(User user, Pageable pageable);
    
    // Find all shares by post
    List<Share> findByPost(Post post);
    
    // Find all shares by post with pagination
    Page<Share> findByPost(Post post, Pageable pageable);
    
    // Find a specific share by user and post
    Optional<Share> findByUserAndPost(User user, Post post);
    
    // Check if a user has shared a specific post
    boolean existsByUserAndPost(User user, Post post);
    
    // Count shares by post
    long countByPost(Post post);
    
    // Delete a share by user and post
    void deleteByUserAndPost(User user, Post post);
}