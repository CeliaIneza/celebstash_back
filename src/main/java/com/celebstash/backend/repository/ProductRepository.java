package com.celebstash.backend.repository;

import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // Find all products with a specific status
    List<Product> findByStatus(ProductStatus status);
    
    // Find all products by seller
    List<Product> findBySeller(User seller);
    
    // Find all products by seller and status
    List<Product> findBySellerAndStatus(User seller, ProductStatus status);
}