package com.celebstash.backend.repository;

import com.celebstash.backend.model.Comment;
import com.celebstash.backend.model.Post;
import com.celebstash.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Find all comments by post
    List<Comment> findByPost(Post post);
    
    // Find all comments by post with pagination
    Page<Comment> findByPost(Post post, Pageable pageable);
    
    // Find all comments by user
    List<Comment> findByUser(User user);
    
    // Find all comments by user with pagination
    Page<Comment> findByUser(User user, Pageable pageable);
    
    // Find all top-level comments (not replies) for a post
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parent IS NULL")
    List<Comment> findTopLevelCommentsByPost(@Param("post") Post post);
    
    // Find all top-level comments for a post with pagination
    @Query("SELECT c FROM Comment c WHERE c.post = :post AND c.parent IS NULL")
    Page<Comment> findTopLevelCommentsByPost(@Param("post") Post post, Pageable pageable);
    
    // Find all replies to a comment
    List<Comment> findByParent(Comment parent);
    
    // Find all replies to a comment with pagination
    Page<Comment> findByParent(Comment parent, Pageable pageable);
    
    // Find all comments liked by a specific user
    @Query("SELECT c FROM Comment c JOIN c.likedBy l WHERE l = :user")
    List<Comment> findCommentsLikedBy(@Param("user") User user);
    
    // Find all comments liked by a specific user with pagination
    @Query("SELECT c FROM Comment c JOIN c.likedBy l WHERE l = :user")
    Page<Comment> findCommentsLikedBy(@Param("user") User user, Pageable pageable);
    
    // Count comments by post
    long countByPost(Post post);
    
    // Count replies to a comment
    long countByParent(Comment parent);
}