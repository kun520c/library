package com.library.service;

import com.library.exception.BusinessException;
import com.library.model.entity.Role;
import com.library.security.AuthenticatedUser;
import com.library.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class BorrowTransactionIntegrationTest {
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private BookService bookService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoBean
    private BookCacheInvalidator cacheInvalidator;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM borrow_records");
        jdbcTemplate.update("DELETE FROM books");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users(id, username, account, password, role) VALUES(1, 'Reader', 'reader', 'hash', 'USER')");
        jdbcTemplate.update("INSERT INTO books(id, title, author, isbn, price, stock, is_deleted) " +
                "VALUES(7, 'Java', 'Author', 'isbn-7', 10.00, 1, 0)");
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void concurrentBorrowNeverMakesStockNegativeOrCreatesExtraRecords() throws Exception {
        int attempts = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    UserContext.set(new AuthenticatedUser(1, "Reader", Role.USER));
                    try {
                        start.await();
                        borrowService.borrow(7);
                        return true;
                    } catch (BusinessException exception) {
                        return false;
                    } finally {
                        UserContext.clear();
                    }
                }));
            }
            start.countDown();

            int successCount = 0;
            for (Future<Boolean> future : futures) {
                successCount += get(future) ? 1 : 0;
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT stock FROM books WHERE id = 7", Integer.class)).isZero();
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM borrow_records", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void recordInsertFailureRollsBackEarlierStockDecrement() {
        UserContext.set(new AuthenticatedUser(999, "missing", Role.USER));

        assertThatThrownBy(() -> borrowService.borrow(7)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT stock FROM books WHERE id = 7", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM borrow_records", Integer.class)).isZero();
    }

    @Test
    void concurrentReturnOnlyRestoresStockOnce() throws Exception {
        jdbcTemplate.update("UPDATE books SET stock = 0 WHERE id = 7");
        jdbcTemplate.update("INSERT INTO borrow_records(id, user_id, book_id, borrow_time, due_time, status) " +
                "VALUES(9, 1, 7, CURRENT_TIMESTAMP, DATEADD('DAY', 30, CURRENT_TIMESTAMP), 'BORROWED')");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    UserContext.set(new AuthenticatedUser(1, "Reader", Role.USER));
                    try {
                        start.await();
                        borrowService.returnBook(9L);
                        return true;
                    } catch (BusinessException exception) {
                        return false;
                    } finally {
                        UserContext.clear();
                    }
                }));
            }
            start.countDown();

            int successCount = 0;
            for (Future<Boolean> future : futures) {
                successCount += get(future) ? 1 : 0;
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT stock FROM books WHERE id = 7", Integer.class)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT status FROM borrow_records WHERE id = 9", String.class)).isEqualTo("RETURNED");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void activeBorrowPreventsBookDeletionButReturnedBookCanBeDeleted() {
        UserContext.set(new AuthenticatedUser(1, "Reader", Role.USER));
        var record = borrowService.borrow(7);

        assertThatThrownBy(() -> bookService.delete(7))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus().value()).isEqualTo(409));

        borrowService.returnBook(record.getRecordId());
        bookService.delete(7);

        assertThat(jdbcTemplate.queryForObject("SELECT is_deleted FROM books WHERE id = 7", Integer.class)).isEqualTo(1);
    }

    private boolean get(Future<Boolean> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw exception;
        }
    }
}
