package com.celebstash.backend.service;

import com.celebstash.backend.exception.AppException;
import com.celebstash.backend.model.User;
import com.celebstash.backend.model.enums.AccountStatus;
import com.celebstash.backend.model.enums.AuthProvider;
import com.celebstash.backend.model.enums.Role;
import com.celebstash.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByEmailOrPhoneNumber(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or phone: " + username));
    }

    @Transactional
    public User createUser(String fullName, String identifier, String password, boolean isEmail) {
        User user = User.builder()
                .fullName(fullName)
                .password(passwordEncoder.encode(password))
                .role(Role.USER)
                .provider(AuthProvider.LOCAL)
                .status(AccountStatus.PENDING)
                .build();

        if (isEmail) {
            user.setEmail(identifier);
            user.setEmailVerified(false);
        } else {
            user.setPhoneNumber(identifier);
            user.setPhoneVerified(false);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserStatus(User user, AccountStatus status) {
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public User verifyUser(User user, boolean isEmail) {
        if (isEmail) {
            user.setEmailVerified(true);
        } else {
            user.setPhoneVerified(true);
        }
        user.setStatus(AccountStatus.VERIFIED);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findByEmailOrPhoneNumber(String identifier) {
        return userRepository.findByEmailOrPhoneNumber(identifier, identifier);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    @Transactional
    public User updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    /**
     * Get the currently authenticated user
     * @return the current user
     * @throws AppException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }

        return userRepository.findByEmailOrPhoneNumber(username, username)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }
}
