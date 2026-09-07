package com.library.service;

import com.library.config.properties.CacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookCacheInvalidator {
    private final StringRedisTemplate redisTemplate;
    private final CacheProperties cacheProperties;

    public void evictAfterCommit(Integer bookId) {
        Runnable eviction = () -> evict(bookId);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }

    public void evict(Integer bookId) {
        String key = cacheProperties.keyPrefix() + bookId;
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("Redis删除失败，cacheKey={}, reason={}", key, exception.getMessage());
        }
    }
}
