package com.celebstash.backend.repository;

import com.celebstash.backend.model.Like;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.LikeableType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // Find all likes by user
    List<Like> findByUser(User user);
    
    // Find all likes by user with pagination
    Page<Like> findByUser(User user, Pageable pageable);
    
    // Find all likes by likeable type and ID
    List<Like> findByLikeableTypeAndLikeableId(LikeableType likeableType, Long likeableId);
    
    // Find all likes by likeable type and ID with pagination
    Page<Like> findByLikeableTypeAndLikeableId(LikeableType likeableType, Long likeableId, Pageable pageable);
    
    // Find a specific like by user, likeable type, and likeable ID
    Optional<Like> findByUserAndLikeableTypeAndLikeableId(User user, LikeableType likeableType, Long likeableId);
    
    // Check if a user has liked a specific entity
    boolean existsByUserAndLikeableTypeAndLikeableId(User user, LikeableType likeableType, Long likeableId);
    
    // Count likes by likeable type and ID
    long countByLikeableTypeAndLikeableId(LikeableType likeableType, Long likeableId);
    
    // Delete a like by user, likeable type, and likeable ID
    void deleteByUserAndLikeableTypeAndLikeableId(User user, LikeableType likeableType, Long likeableId);
}