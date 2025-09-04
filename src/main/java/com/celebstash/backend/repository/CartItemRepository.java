package com.celebstash.backend.repository;

import com.celebstash.backend.model.Cart;
import com.celebstash.backend.model.CartItem;
import com.celebstash.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    // Find all items in a cart
    List<CartItem> findByCart(Cart cart);
    
    // Find a specific product in a cart
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    
    // Check if a product exists in a cart
    boolean existsByCartAndProduct(Cart cart, Product product);
    
    // Delete all items in a cart
    void deleteByCart(Cart cart);
    
    // Delete a specific product from a cart
    void deleteByCartAndProduct(Cart cart, Product product);
}