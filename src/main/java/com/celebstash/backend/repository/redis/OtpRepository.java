package com.celebstash.backend.repository.redis;

import com.celebstash.backend.model.redis.OtpData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends CrudRepository<OtpData, String> {
    
    Optional<OtpData> findById(String id);
    
    Optional<OtpData> findByIdAndOtp(String id, String otp);
}