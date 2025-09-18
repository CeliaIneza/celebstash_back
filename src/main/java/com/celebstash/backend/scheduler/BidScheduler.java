package com.celebstash.backend.scheduler;

import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.Transaction;
import com.celebstash.backend.model.enums.ProductType;
import com.celebstash.backend.model.enums.TransactionStatus;
import com.celebstash.backend.model.enums.TransactionType;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.TransactionRepository;
import com.celebstash.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidScheduler {

    private final ProductRepository productRepository;
    private final WalletService walletService;
    private final TransactionRepository transactionRepository;

    /**
     * Check for completed bids every minute
     * This method finds all bidding products with an end time in the past
     * and logs information about the completed bids
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void checkCompletedBids() {
        log.info("Checking for completed bids...");

        LocalDateTime now = LocalDateTime.now();

        // Find all bidding products with an end time in the past
        List<Product> completedBids = productRepository.findByProductTypeAndBidEndTimeBefore(
                ProductType.BIDDING, now);

        log.info("Found {} completed bids", completedBids.size());

        // Process each completed bid
        for (Product product : completedBids) {
            processCompletedBid(product);
        }
    }

    /**
     * Process a completed bid
     * This method logs information about the completed bid and could be extended
     * to send notifications to the winner and losers
     * @param product the product with a completed bid
     */
    private void processCompletedBid(Product product) {
        if (product.getCurrentBidder() != null) {
            log.info("Bid completed for product {}: Winner is {} with bid amount {}",
                    product.getId(), product.getCurrentBidder().getFullName(), product.getCurrentBidPrice());

            // Find all pending bid transactions for this product
            List<Transaction> bidTransactions = transactionRepository.findByProductAndTypeAndStatus(
                    product, TransactionType.BID, TransactionStatus.PENDING);

            // Process each transaction
            for (Transaction transaction : bidTransactions) {
                // If this is the winner's transaction, mark it as completed
                if (transaction.getWallet().getUser().getId().equals(product.getCurrentBidder().getId())) {
                    transaction.setStatus(TransactionStatus.COMPLETED);
                    transaction.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(transaction);

                    log.info("Completed bid transaction for winner: {}", transaction.getId());
                } else {
                    // For losers, refund their bid amount
                    walletService.refundBid(transaction.getId());
                    log.info("Refunded bid for user: {}", transaction.getWallet().getUser().getId());
                }
            }

            // TODO: Send notification to winner
            // TODO: Send notifications to losers
            // TODO: Move product to a "completed bids" section or mark it as sold
        } else {
            log.info("Bid completed for product {} with no bids", product.getId());

            // TODO: Handle case where no one bid on the product
        }
    }
}
