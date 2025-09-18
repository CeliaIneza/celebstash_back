package com.celebstash.backend.service;

import com.celebstash.backend.dto.bid.BidRequest;
import com.celebstash.backend.dto.bid.BidResponse;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.ProductStatus;
import com.celebstash.backend.model.enums.ProductType;
import com.celebstash.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidService {

    private final ProductRepository productRepository;
    private final UserService userService;
    private final WalletService walletService;

    /**
     * Get all products available for bidding
     * @return list of bidding products
     */
    @Transactional(readOnly = true)
    public List<BidResponse> getBiddingProducts() {
        List<Product> biddingProducts = productRepository.findByProductTypeAndStatus(
                ProductType.BIDDING, ProductStatus.APPROVED);

        return biddingProducts.stream()
                .map(this::mapToBidResponse)
                .collect(Collectors.toList());
    }

    /**
     * Place a bid on a product
     * @param request the bid request
     * @return the updated bid response
     */
    @Transactional
    public BidResponse placeBid(BidRequest request) {
        User currentUser = userService.getCurrentUser();
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Validate product is available for bidding
        if (product.getProductType() != ProductType.BIDDING) {
            throw new AppException("Product is not available for bidding", HttpStatus.BAD_REQUEST);
        }

        // Validate product is approved
        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new AppException("Product is not approved for bidding", HttpStatus.BAD_REQUEST);
        }

        // Check if bidding has ended
        if (product.getBidEndTime() != null && product.getBidEndTime().isBefore(LocalDateTime.now())) {
            throw new AppException("Bidding has ended for this product", HttpStatus.BAD_REQUEST);
        }

        // Check if user has sufficient balance for the bid
        if (!walletService.hasSufficientBalance(request.getBidAmount())) {
            throw new AppException("Insufficient wallet balance. Please top up your wallet.", HttpStatus.BAD_REQUEST);
        }

        // If this is the first bid, start the bidding
        if (product.getBidStartTime() == null) {
            // First bid must be at least the initial price
            if (request.getBidAmount().compareTo(product.getInitialBidPrice()) < 0) {
                throw new AppException("Bid amount must be at least the initial price: " + 
                        product.getInitialBidPrice(), HttpStatus.BAD_REQUEST);
            }

            // Start the bidding
            product.setBidStartTime(LocalDateTime.now());
            product.setBidEndTime(LocalDateTime.now().plusHours(24)); // 24-hour countdown
            product.setCurrentBidPrice(request.getBidAmount());
            product.setCurrentBidder(currentUser);
        } else {
            // Subsequent bids must be higher than the current bid
            if (request.getBidAmount().compareTo(product.getCurrentBidPrice()) <= 0) {
                throw new AppException("Bid amount must be higher than the current bid: " + 
                        product.getCurrentBidPrice(), HttpStatus.BAD_REQUEST);
            }

            // If the current user already has the highest bid, refund their previous bid
            if (currentUser.getId().equals(product.getCurrentBidder().getId())) {
                // Logic to handle refunding previous bid would go here
                // For now, we'll just update the bid without additional transaction
            }

            // Update the current bid
            product.setCurrentBidPrice(request.getBidAmount());
            product.setCurrentBidder(currentUser);
        }

        // Reserve funds for the bid
        walletService.reserveFundsForBid(request.getBidAmount(), product.getId());

        Product updatedProduct = productRepository.save(product);
        return mapToBidResponse(updatedProduct);
    }

    /**
     * Get bid details for a specific product
     * @param productId the product ID
     * @return the bid response
     */
    @Transactional(readOnly = true)
    public BidResponse getBidDetails(Long productId) {
        User currentUser = userService.getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));

        // Validate product is a bidding product
        if (product.getProductType() != ProductType.BIDDING) {
            throw new AppException("Product is not a bidding product", HttpStatus.BAD_REQUEST);
        }

        BidResponse response = mapToBidResponse(product);

        // Check if the current user is the highest bidder
        if (product.getCurrentBidder() != null && 
                product.getCurrentBidder().getId().equals(currentUser.getId())) {
            response.setWinner(isBidWinner(product, currentUser));
        }

        return response;
    }

    /**
     * Check if a user is the winner of a bid
     * @param product the product
     * @param user the user
     * @return true if the user is the winner
     */
    private boolean isBidWinner(Product product, User user) {
        // Bidding must be ended and the user must be the highest bidder
        return product.getBidEndTime() != null && 
               product.getBidEndTime().isBefore(LocalDateTime.now()) &&
               product.getCurrentBidder() != null &&
               product.getCurrentBidder().getId().equals(user.getId());
    }

    /**
     * Map a Product entity to a BidResponse DTO
     * @param product the product entity
     * @return the bid response DTO
     */
    private BidResponse mapToBidResponse(Product product) {
        boolean isActive = product.getBidStartTime() != null && 
                          (product.getBidEndTime() == null || 
                           product.getBidEndTime().isAfter(LocalDateTime.now()));

        String bidStatus = "NOT_STARTED";
        if (product.getBidStartTime() != null) {
            if (isActive) {
                bidStatus = "ACTIVE";
            } else {
                bidStatus = "EXPIRED";
            }
        }

        return BidResponse.builder()
                .productId(product.getId())
                .productName(product.getName())
                .productDescription(product.getDescription())
                .productImageUrl(product.getImageUrl())
                .initialBidPrice(product.getInitialBidPrice())
                .currentBidPrice(product.getCurrentBidPrice())
                .currentBidderId(product.getCurrentBidder() != null ? product.getCurrentBidder().getId() : null)
                .currentBidderName(product.getCurrentBidder() != null ? product.getCurrentBidder().getFullName() : null)
                .bidStartTime(product.getBidStartTime())
                .bidEndTime(product.getBidEndTime())
                .isActive(isActive)
                .bidStatus(bidStatus)
                .build();
    }
}
