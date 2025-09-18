package com.celebstash.backend.controller;

import com.celebstash.backend.dto.cart.CartResponse;
import com.celebstash.backend.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Validated
@Tag(name = "Cart", description = "Cart management APIs")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart items", description = "Returns all items in the user's cart")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CartResponse> getCartItems() {
        return ResponseEntity.ok(cartService.getCartItems());
    }

    @PostMapping("/items")
    @Operation(summary = "Add product to cart", description = "Adds a product to the user's cart")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CartResponse> addProductToCart(
            @RequestParam Long productId,
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
        return ResponseEntity.ok(cartService.addProductToCart(productId, quantity));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update cart item quantity", description = "Updates the quantity of a product in the cart")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CartResponse> updateCartItemQuantity(
            @PathVariable Long productId,
            @RequestParam @Min(value = 0, message = "Quantity must be at least 0") Integer quantity) {
        return ResponseEntity.ok(cartService.updateCartItemQuantity(productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove product from cart", description = "Removes a product from the user's cart")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CartResponse> removeProductFromCart(@PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeProductFromCart(productId));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart", description = "Removes all items from the user's cart")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
