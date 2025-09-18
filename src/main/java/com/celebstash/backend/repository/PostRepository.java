package com.celebstash.backend.repository;

import com.celebstash.backend.model.Post;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Find all posts by user
    List<Post> findByUser(User user);
    
    // Find all posts by user with pagination
    Page<Post> findByUser(User user, Pageable pageable);
    
    // Find post by product
    Optional<Post> findByProduct(Product product);
    
    // Find all posts by users the current user follows
    @Query("SELECT p FROM Post p WHERE p.user IN :users ORDER BY p.createdAt DESC")
    List<Post> findByUsersOrderByCreatedAtDesc(@Param("users") List<User> users);
    
    // Find all posts by users the current user follows with pagination
    @Query("SELECT p FROM Post p WHERE p.user IN :users ORDER BY p.createdAt DESC")
    Page<Post> findByUsersOrderByCreatedAtDesc(@Param("users") List<User> users, Pageable pageable);
    
    // Find all posts liked by a specific user
    @Query("SELECT p FROM Post p JOIN p.likedBy l WHERE l = :user")
    List<Post> findPostsLikedBy(@Param("user") User user);
    
    // Find all posts liked by a specific user with pagination
    @Query("SELECT p FROM Post p JOIN p.likedBy l WHERE l = :user")
    Page<Post> findPostsLikedBy(@Param("user") User user, Pageable pageable);
    
    // Find all posts shared by a specific user
    @Query("SELECT p FROM Post p JOIN p.sharedBy s WHERE s = :user")
    List<Post> findPostsSharedBy(@Param("user") User user);
    
    // Find all posts shared by a specific user with pagination
    @Query("SELECT p FROM Post p JOIN p.sharedBy s WHERE s = :user")
    Page<Post> findPostsSharedBy(@Param("user") User user, Pageable pageable);
}