package com.celebstash.backend.repository;

import com.celebstash.backend.model.Cart;
import com.celebstash.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    
    // Find cart by user
    Optional<Cart> findByUser(User user);
    
    // Check if a cart exists for a user
    boolean existsByUser(User user);
}