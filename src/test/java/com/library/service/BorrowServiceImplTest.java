package com.library.service;

import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.model.dto.BorrowPageDTO;
import com.library.model.entity.Book;
import com.library.model.entity.BorrowRecord;
import com.library.model.entity.BorrowStatus;
import com.library.model.entity.Role;
import com.library.model.vo.BorrowRecordVO;
import com.library.security.AuthenticatedUser;
import com.library.security.UserContext;
import com.library.service.impl.BorrowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T02:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private BookCacheInvalidator cacheInvalidator;
    private BorrowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BorrowServiceImpl(bookMapper, borrowRecordMapper, cacheInvalidator, CLOCK);
        UserContext.set(new AuthenticatedUser(1, "reader", Role.USER));
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void borrowAtomicallyDecrementsStockAndCreatesThirtyDayRecord() {
        when(bookMapper.selectById(7)).thenReturn(book());
        when(bookMapper.decrementStockIfAvailable(7)).thenReturn(1);
        when(borrowRecordMapper.insert(any())).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        });

        var result = service.borrow(7);

        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrowRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1);
        assertThat(captor.getValue().getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(captor.getValue().getDueTime()).isEqualTo(captor.getValue().getBorrowTime().plusDays(30));
        assertThat(result.getRecordId()).isEqualTo(99L);
        verify(cacheInvalidator).evictAfterCommit(7);
    }

    @Test
    void outOfStockDoesNotCreateRecord() {
        when(bookMapper.selectById(7)).thenReturn(book());
        when(bookMapper.decrementStockIfAvailable(7)).thenReturn(0);

        assertConflict(() -> service.borrow(7));
        verify(borrowRecordMapper, never()).insert(any());
    }

    @Test
    void missingBookReturns404WithoutStockUpdate() {
        when(bookMapper.selectById(7)).thenReturn(null);

        assertThatThrownBy(() -> service.borrow(7)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(bookMapper, never()).decrementStockIfAvailable(7);
    }

    @Test
    void returnMarksRecordThenRestoresStockOnce() {
        BorrowRecord record = record(1, BorrowStatus.BORROWED);
        when(borrowRecordMapper.selectById(9L)).thenReturn(record);
        when(borrowRecordMapper.markReturned(any(), any())).thenReturn(1);
        when(bookMapper.incrementStock(7)).thenReturn(1);

        service.returnBook(9L);

        verify(bookMapper).incrementStock(7);
        verify(cacheInvalidator).evictAfterCommit(7);
    }

    @Test
    void duplicateReturnDoesNotIncrementStock() {
        when(borrowRecordMapper.selectById(9L)).thenReturn(record(1, BorrowStatus.RETURNED));

        assertConflict(() -> service.returnBook(9L));
        verify(bookMapper, never()).incrementStock(any());
    }

    @Test
    void userCannotReturnAnotherUsersRecord() {
        when(borrowRecordMapper.selectById(9L)).thenReturn(record(2, BorrowStatus.BORROWED));

        assertThatThrownBy(() -> service.returnBook(9L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(borrowRecordMapper, never()).markReturned(any(), any());
    }

    @Test
    void adminCanReturnAnotherUsersRecord() {
        UserContext.set(new AuthenticatedUser(10, "admin", Role.ADMIN));
        when(borrowRecordMapper.selectById(9L)).thenReturn(record(2, BorrowStatus.BORROWED));
        when(borrowRecordMapper.markReturned(any(), any())).thenReturn(1);
        when(bookMapper.incrementStock(7)).thenReturn(1);

        service.returnBook(9L);

        verify(bookMapper).incrementStock(7);
    }

    @Test
    void myRecordsAreScopedAndOverdueIsComputedDynamically() {
        BorrowRecordVO vo = BorrowRecordVO.builder()
                .status(BorrowStatus.BORROWED)
                .dueTime(LocalDateTime.now(CLOCK).minusDays(1))
                .build();
        BorrowPageDTO query = new BorrowPageDTO(BorrowStatus.BORROWED, " Java ", 1, 10);
        when(borrowRecordMapper.selectByCondition(1, BorrowStatus.BORROWED, "Java", 0L, 10))
                .thenReturn(List.of(vo));
        when(borrowRecordMapper.countByCondition(1, BorrowStatus.BORROWED, "Java")).thenReturn(1L);

        var page = service.myRecords(query);

        assertThat(page.getList().getFirst().isOverdue()).isTrue();
        assertThat(page.getTotal()).isEqualTo(1);
    }

    @Test
    void userCanReadOwnOverdueBorrowDetail() {
        BorrowRecordVO detail = detail(1, BorrowStatus.BORROWED, LocalDateTime.now(CLOCK).minusMinutes(1));
        when(borrowRecordMapper.selectDetailById(9L)).thenReturn(detail);

        BorrowRecordVO result = service.getById(9L);

        assertThat(result).isSameAs(detail);
        assertThat(result.isOverdue()).isTrue();
    }

    @Test
    void adminCanReadAnotherUsersBorrowDetail() {
        UserContext.set(new AuthenticatedUser(10, "admin", Role.ADMIN));
        when(borrowRecordMapper.selectDetailById(9L))
                .thenReturn(detail(2, BorrowStatus.BORROWED, LocalDateTime.now(CLOCK).plusDays(1)));

        BorrowRecordVO result = service.getById(9L);

        assertThat(result.getUserId()).isEqualTo(2);
        assertThat(result.isOverdue()).isFalse();
    }

    @Test
    void userCannotReadAnotherUsersBorrowDetail() {
        when(borrowRecordMapper.selectDetailById(9L))
                .thenReturn(detail(2, BorrowStatus.BORROWED, LocalDateTime.now(CLOCK).minusDays(1)));

        assertThatThrownBy(() -> service.getById(9L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void missingBorrowDetailReturns404() {
        when(borrowRecordMapper.selectDetailById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getById(99L)).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private Book book() {
        return Book.builder().id(7).title("Java").stock(1).isDeleted(0).build();
    }

    private BorrowRecord record(Integer userId, BorrowStatus status) {
        BorrowRecord record = new BorrowRecord();
        record.setId(9L);
        record.setUserId(userId);
        record.setBookId(7);
        record.setStatus(status);
        return record;
    }

    private BorrowRecordVO detail(Integer userId, BorrowStatus status, LocalDateTime dueTime) {
        return BorrowRecordVO.builder()
                .recordId(9L)
                .bookId(7)
                .bookTitle("Java")
                .userId(userId)
                .username("reader")
                .borrowTime(dueTime.minusDays(30))
                .dueTime(dueTime)
                .status(status)
                .build();
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }
}
