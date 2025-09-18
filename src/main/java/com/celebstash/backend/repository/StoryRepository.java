package com.celebstash.backend.repository;

import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.Story;
import com.celebstash.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    // Find all stories by user
    List<Story> findByUser(User user);
    
    // Find all stories by product
    List<Story> findByProduct(Product product);
    
    // Find all active stories (not expired)
    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now")
    List<Story> findActiveStories(@Param("now") LocalDateTime now);
    
    // Find all active stories by user
    @Query("SELECT s FROM Story s WHERE s.user = :user AND s.expiresAt > :now")
    List<Story> findActiveStoriesByUser(@Param("user") User user, @Param("now") LocalDateTime now);
    
    // Find all active stories by users the current user follows
    @Query("SELECT s FROM Story s WHERE s.user IN :users AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUsers(@Param("users") List<User> users, @Param("now") LocalDateTime now);
    
    // Find all stories viewed by a specific user
    @Query("SELECT s FROM Story s JOIN s.viewedBy v WHERE v = :user")
    List<Story> findStoriesViewedBy(@Param("user") User user);
    
    // Find all stories not viewed by a specific user and not expired
    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now AND :user NOT MEMBER OF s.viewedBy")
    List<Story> findStoriesNotViewedBy(@Param("user") User user, @Param("now") LocalDateTime now);
}