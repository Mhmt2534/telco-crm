package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.exception.IdempotencyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String IDEMPOTENCY_PREFIX = "idempotency:payment:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final int TTL_HOURS = 24;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Processes the idempotency key.
     * @param key the idempotency key
     * @return null if request should proceed, or cached JSON response if already completed.
     * @throws IdempotencyException if request is currently IN_PROGRESS
     */
    public String processIdempotency(String key) {
        String fullKey = IDEMPOTENCY_PREFIX + key;
        
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(fullKey, IN_PROGRESS, Duration.ofHours(TTL_HOURS));
        
        if (Boolean.TRUE.equals(isNew)) {
            return null; // It's a new request, proceed
        }
        
        String existingValue = redisTemplate.opsForValue().get(fullKey);
        if (IN_PROGRESS.equals(existingValue)) {
            throw new IdempotencyException("Request with this Idempotency-Key is currently being processed. Please wait.");
        }
        
        return existingValue;
    }

    public void cacheResponse(String key, String responseJson) {
        String fullKey = IDEMPOTENCY_PREFIX + key;
        redisTemplate.opsForValue().set(fullKey, responseJson, Duration.ofHours(TTL_HOURS));
    }

    public void removeKey(String key) {
        String fullKey = IDEMPOTENCY_PREFIX + key;
        redisTemplate.delete(fullKey);
    }
}
