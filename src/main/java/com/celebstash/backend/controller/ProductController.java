package com.celebstash.backend.controller;

import com.celebstash.backend.dto.product.ProductRequest;
import com.celebstash.backend.dto.product.ProductResponse;
import com.celebstash.backend.dto.product.ProductStatusUpdateRequest;
import com.celebstash.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product with PENDING status")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Returns all approved products for regular users, all products for admins")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/my-products")
    @Operation(summary = "Get my products", description = "Returns all products created by the current user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProductResponse>> getMyProducts() {
        return ResponseEntity.ok(productService.getMyProducts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Returns a product by its ID")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update product status", description = "Updates the status of a product (admin only)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProductStatus(id, request));
    }
}