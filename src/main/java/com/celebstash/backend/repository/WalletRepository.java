package com.celebstash.backend.repository;

import com.celebstash.backend.model.User;
import com.celebstash.backend.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    // Find wallet by user
    Optional<Wallet> findByUser(User user);
    
    // Check if a wallet exists for a user
    boolean existsByUser(User user);
}