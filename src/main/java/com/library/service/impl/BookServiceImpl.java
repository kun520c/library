package com.library.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.config.properties.CacheProperties;
import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import com.library.model.dto.BookDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.entity.Book;
import com.library.model.vo.BookVO;
import com.library.model.vo.PageVO;
import com.library.service.BookService;
import com.library.service.BookCacheInvalidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheProperties cacheProperties;
    private final BookCacheInvalidator cacheInvalidator;

    @Override
    @Transactional(readOnly = true)
    public BookVO getById(Integer id) {
        String cacheKey = cacheKey(id);
        String cachedValue = readCache(cacheKey);
        if (cachedValue != null) {
            if (cacheProperties.nullValue().equals(cachedValue)) {
                throw notFound();
            }
            try {
                return objectMapper.readValue(cachedValue, BookVO.class);
            } catch (Exception exception) {
                log.warn("图书缓存JSON损坏，将删除并回源数据库，bookId={}, reason={}", id, exception.getMessage());
                deleteCache(cacheKey);
            }
        }

        Book book = bookMapper.selectById(id);
        if (book == null) {
            writeCache(cacheKey, cacheProperties.nullValue(), cacheProperties.nullTtl());
            throw notFound();
        }

        BookVO bookVO = toVO(book);
        try {
            writeCache(cacheKey, objectMapper.writeValueAsString(bookVO), cacheProperties.ttl());
        } catch (Exception exception) {
            log.warn("图书缓存序列化失败，bookId={}, reason={}", id, exception.getMessage());
        }
        return bookVO;
    }

    @Override
    @Transactional
    public void add(BookDTO dto) {
        validateCategory(dto.getCategoryId());
        if (bookMapper.existsByIsbn(dto.getIsbn())) {
            throw new BusinessException(HttpStatus.CONFLICT, "ISBN已存在");
        }
        Book book = toEntity(dto);
        if (bookMapper.insert(book) != 1) {
            throw new IllegalStateException("新增图书未写入数据库");
        }
        if (book.getId() != null) {
            cacheInvalidator.evictAfterCommit(book.getId());
        }
    }

    @Override
    @Transactional
    public void update(Integer id, BookDTO dto) {
        if (bookMapper.selectById(id) == null) {
            throw notFound();
        }
        validateCategory(dto.getCategoryId());
        if (bookMapper.existsByIsbnAndIdNot(dto.getIsbn(), id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "ISBN已被其他图书使用");
        }
        Book book = toEntity(dto);
        book.setId(id);
        if (bookMapper.update(book) == 0) {
            throw notFound();
        }
        cacheInvalidator.evictAfterCommit(id);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (bookMapper.deleteById(id) == 0) {
            throw notFound();
        }
        cacheInvalidator.evictAfterCommit(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<BookVO> page(BookPageDTO dto) {
        long offset = ((long) dto.getPage() - 1L) * dto.getSize();
        List<BookVO> books = bookMapper.selectByCondition(
                        dto.getTitle(), dto.getAuthor(), dto.getIsbn(), dto.getCategoryId(), offset, dto.getSize())
                .stream()
                .map(this::toVO)
                .toList();
        long total = bookMapper.countByCondition(dto.getTitle(), dto.getAuthor(), dto.getIsbn(), dto.getCategoryId());
        return new PageVO<>(books, total, dto.getPage(), dto.getSize());
    }

    private String readCache(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception exception) {
            log.warn("Redis读取失败，将回源数据库，cacheKey={}, reason={}", cacheKey, exception.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, String value, java.time.Duration ttl) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
        } catch (Exception exception) {
            log.warn("Redis写入失败，cacheKey={}, reason={}", cacheKey, exception.getMessage());
        }
    }

    private void deleteCache(String cacheKey) {
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception exception) {
            log.warn("Redis删除失败，cacheKey={}, reason={}", cacheKey, exception.getMessage());
        }
    }

    private void validateCategory(Integer categoryId) {
        if (categoryId != null && categoryMapper.selectByIdForUpdate(categoryId) == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "分类不存在");
        }
    }

    private String cacheKey(Integer id) {
        return cacheProperties.keyPrefix() + id;
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "图书不存在");
    }

    private BookVO toVO(Book book) {
        return BookVO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .price(book.getPrice())
                .stock(book.getStock())
                .categoryId(book.getCategoryId())
                .build();
    }

    private Book toEntity(BookDTO dto) {
        return Book.builder()
                .title(dto.getTitle())
                .author(dto.getAuthor())
                .isbn(dto.getIsbn())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .categoryId(dto.getCategoryId())
                .build();
    }
}
