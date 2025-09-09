package com.celebstash.backend.controller;

import com.celebstash.backend.dto.bid.BidRequest;
import com.celebstash.backend.dto.bid.BidResponse;
import com.celebstash.backend.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
@Tag(name = "Bidding", description = "Bidding management APIs")
public class BidController {

    private final BidService bidService;

    @GetMapping
    @Operation(summary = "Get all bidding products", description = "Returns all products available for bidding")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<BidResponse>> getBiddingProducts() {
        return ResponseEntity.ok(bidService.getBiddingProducts());
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get bid details", description = "Returns details of a specific bid")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BidResponse> getBidDetails(@PathVariable Long productId) {
        return ResponseEntity.ok(bidService.getBidDetails(productId));
    }

    @PostMapping
    @Operation(summary = "Place a bid", description = "Places a bid on a product")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BidResponse> placeBid(@Valid @RequestBody BidRequest request) {
        return ResponseEntity.ok(bidService.placeBid(request));
    }
}