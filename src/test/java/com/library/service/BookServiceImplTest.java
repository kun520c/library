package com.library.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.config.properties.CacheProperties;
import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.mapper.CategoryMapper;
import com.library.model.dto.BookCreateDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.dto.BookUpdateDTO;
import com.library.model.dto.StockAdjustmentDTO;
import com.library.model.entity.Book;
import com.library.model.entity.Category;
import com.library.model.vo.BookVO;
import com.library.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {
    private static final String CACHE_KEY = "test:book:1";

    @Mock
    private BookMapper bookMapper;
    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private BookCacheInvalidator cacheInvalidator;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private BookServiceImpl service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(categoryMapper.selectByIdForUpdate(2)).thenReturn(new Category());
        service = new BookServiceImpl(bookMapper, borrowRecordMapper, categoryMapper, redisTemplate, objectMapper,
                new CacheProperties("test:book:", "__NULL__", Duration.ofMinutes(10), Duration.ofMinutes(1)),
                cacheInvalidator);
    }

    @Test
    void returnsBookFromIdCache() throws Exception {
        BookVO cached = bookVO();
        when(valueOperations.get(CACHE_KEY)).thenReturn(objectMapper.writeValueAsString(cached));

        assertThat(service.getById(1)).usingRecursiveComparison().isEqualTo(cached);
        verify(bookMapper, never()).selectById(1);
    }

    @Test
    void cacheMissReadsDatabaseAndWritesCache() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(bookMapper.selectById(1)).thenReturn(book());

        BookVO result = service.getById(1);

        assertThat(result.getIsbn()).isEqualTo("978-7-111");
        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), eq(Duration.ofMinutes(10)));
    }

    @Test
    void cachesMissingBookForShortTtl() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(bookMapper.selectById(1)).thenReturn(null);

        assertNotFound(() -> service.getById(1));

        verify(valueOperations).set(CACHE_KEY, "__NULL__", Duration.ofMinutes(1));
    }

    @Test
    void nullMarkerAvoidsDatabaseLookup() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("__NULL__");

        assertNotFound(() -> service.getById(1));
        verify(bookMapper, never()).selectById(1);
    }

    @Test
    void redisFailureFallsBackToDatabase() {
        when(valueOperations.get(CACHE_KEY)).thenThrow(new IllegalStateException("redis down"));
        when(bookMapper.selectById(1)).thenReturn(book());

        assertThat(service.getById(1).getId()).isEqualTo(1);
        verify(bookMapper).selectById(1);
    }

    @Test
    void corruptJsonIsDeletedBeforeDatabaseFallback() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("{broken-json");
        when(bookMapper.selectById(1)).thenReturn(book());

        assertThat(service.getById(1).getId()).isEqualTo(1);

        verify(redisTemplate).delete(CACHE_KEY);
        verify(bookMapper).selectById(1);
    }

    @Test
    void addingDuplicateIsbnReturnsConflict() {
        when(bookMapper.existsByIsbn("978-7-111")).thenReturn(true);

        assertConflict(() -> service.add(bookCreateDto()));
        verify(bookMapper, never()).insert(any());
    }

    @Test
    void addingBookWithMissingCategoryReturnsBadRequest() {
        when(categoryMapper.selectByIdForUpdate(2)).thenReturn(null);

        assertThatThrownBy(() -> service.add(bookCreateDto()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(bookMapper, never()).insert(any());
    }

    @Test
    void addInvalidatesPossibleNegativeCacheForGeneratedId() {
        when(bookMapper.existsByIsbn("978-7-111")).thenReturn(false);
        doAnswer(invocation -> {
            Book inserted = invocation.getArgument(0);
            inserted.setId(1);
            return 1;
        }).when(bookMapper).insert(any(Book.class));
        service.add(bookCreateDto());

        verify(cacheInvalidator).evictAfterCommit(1);
    }

    @Test
    void updatingMissingBookReturnsNotFound() {
        when(bookMapper.selectById(1)).thenReturn(null);

        assertNotFound(() -> service.update(1, bookUpdateDto()));
        verify(bookMapper, never()).updateBasicInfo(any());
    }

    @Test
    void updatingToAnotherBooksIsbnReturnsConflict() {
        when(bookMapper.selectById(1)).thenReturn(book());
        when(bookMapper.existsByIsbnAndIdNot("978-7-111", 1)).thenReturn(true);

        assertConflict(() -> service.update(1, bookUpdateDto()));
        verify(bookMapper, never()).updateBasicInfo(any());
    }

    @Test
    void deletingMissingOrAlreadyDeletedBookReturnsNotFound() {
        when(bookMapper.selectByIdForUpdate(1)).thenReturn(null);

        assertNotFound(() -> service.delete(1));
        verify(bookMapper, never()).deleteById(1);
    }

    @Test
    void deletingBookWithActiveBorrowReturnsConflict() {
        when(bookMapper.selectByIdForUpdate(1)).thenReturn(book());
        when(borrowRecordMapper.existsBorrowedByBookId(1)).thenReturn(true);

        assertConflict(() -> service.delete(1));

        verify(bookMapper, never()).deleteById(1);
    }

    @Test
    void updateInvalidatesIdCache() {
        when(bookMapper.selectById(1)).thenReturn(book());
        when(bookMapper.existsByIsbnAndIdNot("978-7-111", 1)).thenReturn(false);
        when(bookMapper.updateBasicInfo(any())).thenReturn(1);
        service.update(1, bookUpdateDto());

        verify(cacheInvalidator).evictAfterCommit(1);
    }

    @Test
    void updateBasicInfoNeverPassesAnAbsoluteStockValue() {
        when(bookMapper.selectById(1)).thenReturn(book());
        when(bookMapper.existsByIsbnAndIdNot("978-7-111", 1)).thenReturn(false);
        when(bookMapper.updateBasicInfo(any())).thenReturn(1);

        service.update(1, bookUpdateDto());

        verify(bookMapper).updateBasicInfo(org.mockito.ArgumentMatchers.argThat(updated -> updated.getStock() == null));
    }

    @Test
    void adjustsStockByDeltaAndInvalidatesCache() {
        when(bookMapper.adjustStock(1, 5)).thenReturn(1);

        service.adjustStock(1, new StockAdjustmentDTO(5));

        verify(cacheInvalidator).evictAfterCommit(1);
    }

    @Test
    void rejectsAdjustmentThatWouldMakeStockNegative() {
        when(bookMapper.adjustStock(1, -6)).thenReturn(0);
        when(bookMapper.selectById(1)).thenReturn(book());

        assertConflict(() -> service.adjustStock(1, new StockAdjustmentDTO(-6)));
    }

    @Test
    void adjustingMissingBookReturnsNotFound() {
        when(bookMapper.adjustStock(1, 2)).thenReturn(0);
        when(bookMapper.selectById(1)).thenReturn(null);

        assertNotFound(() -> service.adjustStock(1, new StockAdjustmentDTO(2)));
    }

    @Test
    void zeroStockAdjustmentReturnsBadRequest() {
        assertThatThrownBy(() -> service.adjustStock(1, new StockAdjustmentDTO(0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(bookMapper, never()).adjustStock(any(), any());
    }

    @Test
    void deleteInvalidatesIdCache() {
        when(bookMapper.selectByIdForUpdate(1)).thenReturn(book());
        when(borrowRecordMapper.existsBorrowedByBookId(1)).thenReturn(false);
        when(bookMapper.deleteById(1)).thenReturn(1);

        service.delete(1);

        verify(cacheInvalidator).evictAfterCommit(1);
    }

    @Test
    void pageUsesLongOffsetAndReturnsTotalAndRequestCoordinates() {
        BookPageDTO query = new BookPageDTO(" Spring ", null, " 978-7-111 ", null, 3, 20);
        when(bookMapper.selectByCondition(eq("Spring"), eq(null), eq("978-7-111"), eq(null), anyLong(), eq(20)))
                .thenReturn(List.of(book()));
        when(bookMapper.countByCondition("Spring", null, "978-7-111", null)).thenReturn(41L);

        var page = service.page(query);

        ArgumentCaptor<Long> offset = ArgumentCaptor.forClass(Long.class);
        verify(bookMapper).selectByCondition(eq("Spring"), eq(null), eq("978-7-111"), eq(null), offset.capture(), eq(20));
        assertThat(offset.getValue()).isEqualTo(40L);
        assertThat(page.getTotal()).isEqualTo(41L);
        assertThat(page.getPage()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getList()).hasSize(1);
    }

    @Test
    void nullPageAndSizeUseDefaultsWithoutNullPointerException() {
        BookPageDTO query = new BookPageDTO(null, null, null, null, null, null);
        when(bookMapper.selectByCondition(null, null, null, null, 0L, 10)).thenReturn(List.of());
        when(bookMapper.countByCondition(null, null, null, null)).thenReturn(0L);

        var page = service.page(query);

        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    private Book book() {
        return Book.builder()
                .id(1)
                .title("Spring")
                .author("Author")
                .isbn("978-7-111")
                .price(new BigDecimal("59.90"))
                .stock(5)
                .categoryId(2)
                .isDeleted(0)
                .build();
    }

    private BookVO bookVO() {
        return BookVO.builder()
                .id(1)
                .title("Spring")
                .author("Author")
                .isbn("978-7-111")
                .price(new BigDecimal("59.90"))
                .stock(5)
                .categoryId(2)
                .build();
    }

    private BookCreateDTO bookCreateDto() {
        return new BookCreateDTO("Spring", "Author", "978-7-111", new BigDecimal("59.90"), 5, 2);
    }

    private BookUpdateDTO bookUpdateDto() {
        return new BookUpdateDTO("Spring", "Author", "978-7-111", new BigDecimal("59.90"), 2);
    }
}
