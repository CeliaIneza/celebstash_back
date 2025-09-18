package com.celebstash.backend.repository.redis;

import com.celebstash.backend.model.redis.RateLimitData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitRepository extends CrudRepository<RateLimitData, String> {
    
    Optional<RateLimitData> findById(String id);
}