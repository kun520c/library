package com.library.service;

import com.library.config.properties.CacheProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookCacheInvalidatorTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void evictionRunsOnlyAfterTransactionCommit() {
        BookCacheInvalidator invalidator = new BookCacheInvalidator(redisTemplate, properties());
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        invalidator.evictAfterCommit(7);

        verify(redisTemplate, never()).delete("test:book:7");
        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
        verify(redisTemplate).delete("test:book:7");
    }

    @Test
    void redisFailureDoesNotEscape() {
        BookCacheInvalidator invalidator = new BookCacheInvalidator(redisTemplate, properties());
        org.mockito.Mockito.doThrow(new IllegalStateException("redis down"))
                .when(redisTemplate).delete("test:book:7");

        org.assertj.core.api.Assertions.assertThatCode(() -> invalidator.evict(7)).doesNotThrowAnyException();
    }

    private CacheProperties properties() {
        return new CacheProperties("test:book:", "__NULL__", Duration.ofMinutes(10), Duration.ofMinutes(1));
    }
}
