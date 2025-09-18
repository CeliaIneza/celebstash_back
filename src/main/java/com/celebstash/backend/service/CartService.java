package com.celebstash.backend.service;

import com.celebstash.backend.dto.cart.CartItemResponse;
import com.celebstash.backend.dto.cart.CartResponse;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Cart;
import com.celebstash.backend.model.CartItem;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.ProductStatus;
import com.celebstash.backend.repository.CartItemRepository;
import com.celebstash.backend.repository.CartRepository;
import com.celebstash.backend.repository.ProductRepository;
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
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final WalletService walletService;

    /**
     * Get or create a cart for the current user
     * @return the user's cart
     */
    @Transactional
    public Cart getOrCreateCart() {
        User currentUser = userService.getCurrentUser();
        return cartRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(currentUser)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Add a product to the user's cart
     * @param productId the ID of the product to add
     * @param quantity the quantity to add
     * @return the updated cart
     */
    @Transactional
    public CartResponse addProductToCart(Long productId, Integer quantity) {
        if (quantity <= 0) {
            throw new AppException("Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Only approved products can be added to cart
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new AppException("Product is not available for purchase", HttpStatus.BAD_REQUEST);
        }

        // Check if there's enough stock
        if (product.getStockQuantity() < quantity) {
            throw new AppException("Not enough stock available", HttpStatus.BAD_REQUEST);
        }

        // Calculate total price for this product
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        // Check if user has sufficient balance
        if (!walletService.hasSufficientBalance(totalPrice)) {
            throw new AppException("Insufficient wallet balance. Please top up your wallet.", HttpStatus.BAD_REQUEST);
        }

        Cart cart = getOrCreateCart();

        // Check if product already exists in cart
        CartItem existingItem = cartItemRepository.findByCartAndProduct(cart, product).orElse(null);

        if (existingItem != null) {
            // Update quantity if product already in cart
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            cartItemRepository.save(existingItem);
        } else {
            // Add new item to cart
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
        }

        return mapToCartResponse(cart);
    }

    /**
     * Remove a product from the user's cart
     * @param productId the ID of the product to remove
     * @return the updated cart
     */
    @Transactional
    public CartResponse removeProductFromCart(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        Cart cart = getOrCreateCart();

        cartItemRepository.deleteByCartAndProduct(cart, product);

        return mapToCartResponse(cart);
    }

    /**
     * Update the quantity of a product in the cart
     * @param productId the ID of the product to update
     * @param quantity the new quantity
     * @return the updated cart
     */
    @Transactional
    public CartResponse updateCartItemQuantity(Long productId, Integer quantity) {
        if (quantity <= 0) {
            return removeProductFromCart(productId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Check if there's enough stock
        if (product.getStockQuantity() < quantity) {
            throw new AppException("Not enough stock available", HttpStatus.BAD_REQUEST);
        }

        // Calculate total price for this product
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(quantity));

        // Check if user has sufficient balance
        if (!walletService.hasSufficientBalance(totalPrice)) {
            throw new AppException("Insufficient wallet balance. Please top up your wallet.", HttpStatus.BAD_REQUEST);
        }

        Cart cart = getOrCreateCart();

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new AppException("Product not found in cart", HttpStatus.NOT_FOUND));

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return mapToCartResponse(cart);
    }

    /**
     * Get all items in the user's cart
     * @return cart response with items
     */
    @Transactional(readOnly = true)
    public CartResponse getCartItems() {
        Cart cart = getOrCreateCart();
        return mapToCartResponse(cart);
    }

    /**
     * Clear the user's cart
     */
    @Transactional
    public void clearCart() {
        Cart cart = getOrCreateCart();
        cartItemRepository.deleteByCart(cart);
    }

    /**
     * Map a Cart entity to a CartResponse DTO
     * @param cart the cart entity
     * @return the cart response DTO
     */
    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItem> cartItems = cartItemRepository.findByCart(cart);
        List<CartItemResponse> cartItemResponses = cartItems.stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        // Calculate total items and total price
        int totalItems = cartItemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalPrice = cartItemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(cartItemResponses)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }

    /**
     * Map a CartItem entity to a CartItemResponse DTO
     * @param cartItem the cart item entity
     * @return the cart item response DTO
     */
    private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .productImageUrl(product.getImageUrl())
                .productPrice(product.getPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}
