package com.celebstash.backend.service;

import com.celebstash.backend.dto.wallet.TopUpRequest;
import com.celebstash.backend.dto.wallet.TransactionResponse;
import com.celebstash.backend.dto.wallet.WalletResponse;
import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.Product;
import com.celebstash.backend.model.Transaction;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.Wallet;
import com.celebstash.backend.model.enums.TransactionStatus;
import com.celebstash.backend.model.enums.TransactionType;
import com.celebstash.backend.repository.ProductRepository;
import com.celebstash.backend.repository.TransactionRepository;
import com.celebstash.backend.repository.WalletRepository;
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
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    /**
     * Get or create a wallet for the current user
     * @return the user's wallet
     */
    @Transactional
    public Wallet getOrCreateWallet() {
        User currentUser = userService.getCurrentUser();
        return walletRepository.findByUser(currentUser)
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(currentUser)
                            .balance(BigDecimal.ZERO)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    /**
     * Get the current user's wallet information
     * @return wallet response with balance
     */
    @Transactional(readOnly = true)
    public WalletResponse getWalletInfo() {
        Wallet wallet = getOrCreateWallet();
        return mapToWalletResponse(wallet);
    }

    /**
     * Add funds to the user's wallet
     * @param request the top-up request
     * @return the updated wallet response
     */
    @Transactional
    public WalletResponse topUpWallet(TopUpRequest request) {
        Wallet wallet = getOrCreateWallet();
        
        // Create a deposit transaction
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet top-up")
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        
        // Update wallet balance
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        
        // Save transaction and wallet
        transactionRepository.save(transaction);
        walletRepository.save(wallet);
        
        return mapToWalletResponse(wallet);
    }

    /**
     * Check if the user has sufficient balance for a purchase
     * @param amount the amount to check
     * @return true if the user has sufficient balance
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(BigDecimal amount) {
        Wallet wallet = getOrCreateWallet();
        return wallet.getBalance().compareTo(amount) >= 0;
    }

    /**
     * Deduct funds from the user's wallet for a purchase
     * @param amount the amount to deduct
     * @param productId the ID of the product being purchased
     * @return the updated wallet response
     */
    @Transactional
    public WalletResponse deductFunds(BigDecimal amount, Long productId) {
        Wallet wallet = getOrCreateWallet();
        
        // Check if user has sufficient balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }
        
        // Get the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        
        // Create a purchase transaction
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.PURCHASE)
                .status(TransactionStatus.COMPLETED)
                .description("Purchase of " + product.getName())
                .product(product)
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        
        // Update wallet balance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        
        // Save transaction and wallet
        transactionRepository.save(transaction);
        walletRepository.save(wallet);
        
        return mapToWalletResponse(wallet);
    }

    /**
     * Reserve funds for a bid (create a pending transaction)
     * @param amount the bid amount
     * @param productId the ID of the product being bid on
     * @return the transaction response
     */
    @Transactional
    public TransactionResponse reserveFundsForBid(BigDecimal amount, Long productId) {
        Wallet wallet = getOrCreateWallet();
        
        // Check if user has sufficient balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new AppException("Insufficient balance", HttpStatus.BAD_REQUEST);
        }
        
        // Get the product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Product not found", HttpStatus.NOT_FOUND));
        
        // Create a bid transaction
        Transaction transaction = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(TransactionType.BID)
                .status(TransactionStatus.PENDING)
                .description("Bid on " + product.getName())
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Update wallet balance
        wallet.setBalance(wallet.getBalance().subtract(amount));
        
        // Save transaction and wallet
        Transaction savedTransaction = transactionRepository.save(transaction);
        walletRepository.save(wallet);
        
        return mapToTransactionResponse(savedTransaction);
    }

    /**
     * Refund a bid amount to the user's wallet
     * @param transactionId the ID of the transaction to refund
     * @return the updated wallet response
     */
    @Transactional
    public WalletResponse refundBid(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException("Transaction not found", HttpStatus.NOT_FOUND));
        
        // Verify this is a bid transaction
        if (transaction.getType() != TransactionType.BID) {
            throw new AppException("Not a bid transaction", HttpStatus.BAD_REQUEST);
        }
        
        // Update transaction status
        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setCompletedAt(LocalDateTime.now());
        
        // Create a refund transaction
        Transaction refundTransaction = Transaction.builder()
                .wallet(transaction.getWallet())
                .amount(transaction.getAmount())
                .type(TransactionType.BID_REFUND)
                .status(TransactionStatus.COMPLETED)
                .description("Refund for bid on " + 
                        (transaction.getProduct() != null ? transaction.getProduct().getName() : "product"))
                .product(transaction.getProduct())
                .createdAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        
        // Update wallet balance
        Wallet wallet = transaction.getWallet();
        wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
        
        // Save transactions and wallet
        transactionRepository.save(transaction);
        transactionRepository.save(refundTransaction);
        walletRepository.save(wallet);
        
        return mapToWalletResponse(wallet);
    }

    /**
     * Get transaction history for the current user
     * @return list of transaction responses
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory() {
        Wallet wallet = getOrCreateWallet();
        List<Transaction> transactions = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet);
        
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Map a Wallet entity to a WalletResponse DTO
     * @param wallet the wallet entity
     * @return the wallet response DTO
     */
    private WalletResponse mapToWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .userName(wallet.getUser().getFullName())
                .balance(wallet.getBalance())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    /**
     * Map a Transaction entity to a TransactionResponse DTO
     * @param transaction the transaction entity
     * @return the transaction response DTO
     */
    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        TransactionResponse.TransactionResponseBuilder builder = TransactionResponse.builder()
                .id(transaction.getId())
                .walletId(transaction.getWallet().getId())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .status(transaction.getStatus())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .completedAt(transaction.getCompletedAt());
        
        // Add product information if available
        if (transaction.getProduct() != null) {
            builder.productId(transaction.getProduct().getId())
                   .productName(transaction.getProduct().getName());
        }
        
        return builder.build();
    }
}