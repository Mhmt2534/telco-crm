package com.telcox.springmicroservices.payment.service;

import com.telcox.springmicroservices.payment.exception.IdempotencyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class IdempotencyServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private IdempotencyService idempotencyService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOperations = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        idempotencyService = new IdempotencyService(redisTemplate);
    }

    @Test
    void testProcessIdempotency_NewKey() {
        String key = "testKey123";
        when(valueOperations.setIfAbsent(eq("idempotency:payment:" + key), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(true);

        String result = idempotencyService.processIdempotency(key);
        
        assertNull(result, "Expected null indicating new request");
    }

    @Test
    void testProcessIdempotency_InProgressKey() {
        String key = "testKey123";
        String fullKey = "idempotency:payment:" + key;
        when(valueOperations.setIfAbsent(eq(fullKey), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get(fullKey)).thenReturn("IN_PROGRESS");

        assertThrows(IdempotencyException.class, () -> idempotencyService.processIdempotency(key));
    }

    @Test
    void testProcessIdempotency_CompletedKey() {
        String key = "testKey123";
        String fullKey = "idempotency:payment:" + key;
        String cachedResponse = "{\"status\":\"COMPLETED\"}";
        
        when(valueOperations.setIfAbsent(eq(fullKey), eq("IN_PROGRESS"), any(Duration.class)))
                .thenReturn(false);
        when(valueOperations.get(fullKey)).thenReturn(cachedResponse);

        String result = idempotencyService.processIdempotency(key);
        
        assertEquals(cachedResponse, result);
    }
}
