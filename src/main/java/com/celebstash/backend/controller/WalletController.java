package com.celebstash.backend.controller;

import com.celebstash.backend.dto.wallet.TopUpRequest;
import com.celebstash.backend.dto.wallet.TransactionResponse;
import com.celebstash.backend.dto.wallet.WalletResponse;
import com.celebstash.backend.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management APIs")
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get wallet information", description = "Returns the current user's wallet information")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WalletResponse> getWalletInfo() {
        return ResponseEntity.ok(walletService.getWalletInfo());
    }

    @PostMapping("/top-up")
    @Operation(summary = "Top up wallet", description = "Adds funds to the user's wallet")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WalletResponse> topUpWallet(@Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUpWallet(request));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get transaction history", description = "Returns the user's transaction history")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory() {
        return ResponseEntity.ok(walletService.getTransactionHistory());
    }
}