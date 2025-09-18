package com.celebstash.backend.service;

import com.celebstash.backend.dto.product.ProductRequest;
import com.celebstash.backend.dto.product.ProductResponse;
import com.celebstash.backend.dto.product.ProductStatusUpdateRequest;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.ProductStatus;
import com.celebstash.backend.model.enums.ProductType;
import com.celebstash.backend.model.enums.Role;
import com.celebstash.backend.repository.PostRepository;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PostRepository postRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        User currentUser = userService.getCurrentUser();

        Product.ProductBuilder productBuilder = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .stockQuantity(request.getStockQuantity())
                .status(ProductStatus.PENDING) // All new products start as PENDING
                .productType(request.getProductType()) // Set product type from request
                .seller(currentUser)
                .createdAt(LocalDateTime.now());

        // If it's a bidding product, set the initial bid price
        if (request.getProductType() == ProductType.BIDDING) {
            if (request.getInitialBidPrice() == null) {
                throw new AppException("Initial bid price is required for bidding products", HttpStatus.BAD_REQUEST);
            }
            productBuilder.initialBidPrice(request.getInitialBidPrice());
        }

        Product product = productBuilder.build();
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

    /**
     * Move a product to the bidding section
     * @param productId the ID of the product to move
     * @param initialBidPrice the initial bid price
     * @return the updated product
     */
    @Transactional
    public ProductResponse moveProductToBidding(Long productId, BigDecimal initialBidPrice) {
        User currentUser = userService.getCurrentUser();

        // Only admins can move products to bidding
        if (currentUser.getRole() != Role.ADMIN) {
            throw new AppException("Only admins can move products to bidding", HttpStatus.FORBIDDEN);
        }

        if (initialBidPrice == null || initialBidPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Initial bid price must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Only approved products can be moved to bidding
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new AppException("Only approved products can be moved to bidding", HttpStatus.BAD_REQUEST);
        }

        // Product must be a regular product
        if (product.getProductType() == ProductType.BIDDING) {
            throw new AppException("Product is already in bidding", HttpStatus.BAD_REQUEST);
        }

        // Move product to bidding
        product.setProductType(ProductType.BIDDING);
        product.setInitialBidPrice(initialBidPrice);

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    private ProductResponse mapToProductResponse(Product product) {
        ProductResponse.ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .stockQuantity(product.getStockQuantity())
                .status(product.getStatus())
                .productType(product.getProductType())
                .sellerId(product.getSeller().getId())
                .sellerName(product.getSeller().getFullName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .approvedAt(product.getApprovedAt())
                .hasPost(false);

        // Check if a post exists for this product
        postRepository.findByProduct(product).ifPresent(post -> {
            builder.hasPost(true);
            builder.postId(post.getId());
        });

        // Add bidding-related fields if it's a bidding product
        if (product.getProductType() == ProductType.BIDDING) {
            builder.initialBidPrice(product.getInitialBidPrice())
                   .currentBidPrice(product.getCurrentBidPrice());

            // Add bidder information if there is a current bidder
            if (product.getCurrentBidder() != null) {
                builder.currentBidderId(product.getCurrentBidder().getId())
                       .currentBidderName(product.getCurrentBidder().getFullName());
            }

            builder.bidStartTime(product.getBidStartTime())
                   .bidEndTime(product.getBidEndTime());

            // Check if bidding is active
            boolean isBiddingActive = product.getBidStartTime() != null && 
                                     (product.getBidEndTime() == null || 
                                      product.getBidEndTime().isAfter(LocalDateTime.now()));
            builder.isBiddingActive(isBiddingActive);
        }

        return builder.build();
    }
}
