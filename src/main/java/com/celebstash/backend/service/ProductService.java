package com.celebstash.backend.service;

import com.celebstash.backend.dto.product.ProductRequest;
import com.celebstash.backend.dto.product.ProductResponse;
import com.celebstash.backend.dto.product.ProductStatusUpdateRequest;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.ProductStatus;
import com.celebstash.backend.model.enums.Role;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = userService.getCurrentUser();
        
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity())
                .status(ProductStatus.PENDING) // All new products start as PENDING
                .seller(currentUser)
                .createdAt(LocalDateTime.now())
                .build();
        
        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }
    
    @Transactional
    public ProductResponse updateProductStatus(Long productId, ProductStatusUpdateRequest request) {
        User currentUser = userService.getCurrentUser();
        
        // Only admins can update product status
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AppException("Only admins can approve or reject products", HttpStatus.FORBIDDEN);
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        
        product.setStatus(request.getStatus());
        
        if (request.getStatus() == ProductStatus.APPROVED) {
            product.setApprovedAt(LocalDateTime.now());
        }
        
        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        User currentUser = userService.getCurrentUser();
        List<Product> products;
        
        // Admins can see all products, regular users can only see approved products
        if (currentUser.getRole() == Role.ADMIN) {
            products = productRepository.findAll();
        } else {
            products = productRepository.findByStatus(ProductStatus.APPROVED);
        }
        
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getMyProducts() {
        User currentUser = userService.getCurrentUser();
        List<Product> products = productRepository.findBySeller(currentUser);
        
        return products.stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        User currentUser = userService.getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        
        // Regular users can only see approved products unless they are the seller
        if (currentUser.getRole() != Role.ADMIN && 
            !product.getSeller().getId().equals(currentUser.getId()) && 
            product.getStatus() != ProductStatus.APPROVED) {
            throw new AppException("Product not found", HttpStatus.NOT_FOUND);
        }
        
        return mapToProductResponse(product);
    }
    
    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .approvedAt(product.getApprovedAt())
                .build();
    }
}