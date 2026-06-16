package com.library.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.model.entity.Book;
import com.library.model.dto.BookDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.vo.BookVO;
import com.library.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 图书 Service 实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookMapper bookMapper;

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private static final String BOOK_LIST_KEY = "book:list";

    private static final String BOOK_KEY_PREFIX = "book:";

    @Override
    public List<BookVO> list() {
        try {
            String cache = redisTemplate.opsForValue().get(BOOK_LIST_KEY);
            if (cache != null) {
                return objectMapper.readValue(cache, new TypeReference<List<BookVO>>() {});
            }
        } catch (Exception e) {
            log.warn("缓存解析失败",e);
        }

        List<BookVO> list = bookMapper.selectAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        try {
            redisTemplate.opsForValue().set(BOOK_LIST_KEY, objectMapper.writeValueAsString(list), 10, TimeUnit.MINUTES);
        } catch (Exception ignored) {
            log.warn("缓存写入失败",ignored);
        }
        return list;
    }

    @Override
    public BookVO getById(Integer id) {
        String KEY = BOOK_KEY_PREFIX + id;

        try {
            String cache = redisTemplate.opsForValue().get(KEY);
            if (cache != null) {
                if ("null".equals(cache)) {
                    throw new BusinessException(404, "图书不存在");
                }
                return objectMapper.readValue(cache, BookVO.class);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("缓存解析失败", e);
        }

        Book book = bookMapper.selectById(id);

        if (book == null) {
            try {
                redisTemplate.opsForValue().set(KEY,"null",1,TimeUnit.MINUTES);
            }catch (Exception ignored){
                log.warn("缓存写入失败",ignored);
            }
            throw new BusinessException(404, "图书不存在");
        }

        try {
            redisTemplate.opsForValue().set(KEY,objectMapper.writeValueAsString(toVO(book)),10,TimeUnit.MINUTES);
        }catch (Exception ignored){
            log.warn("写入缓存失败",ignored);
        }


        return toVO(book);
    }

    public Book getByIsbn(String isbn){
        Book book = bookMapper.selectByIsbn(isbn);
        return book;
    }

    @Override
    @Transactional
    public void add(BookDTO dto) {
        if(getByIsbn(dto.getIsbn()) == null){
            Book book = toEntity(dto);
            bookMapper.insert(book);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    clearListCache();
                }
            });
        }else{
            throw new BusinessException(409,"该书已存入");
        }
    }

    @Override
    @Transactional
    public void update(Integer id, BookDTO dto) {
        Book exist = bookMapper.selectById(id);
        String KEY = BOOK_KEY_PREFIX + id;
        if (exist == null) {
            throw new BusinessException(404, "图书不存在");
        }
        Book book = toEntity(dto);
        book.setId(id);
        bookMapper.update(book);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearListCache();
                clearIdCache(KEY);
            }
        });
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Book exist = bookMapper.selectById(id);
        String KEY = BOOK_KEY_PREFIX + id;
        if (exist == null) {
            throw new BusinessException(404, "图书不存在");
        }
        bookMapper.deleteById(id);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clearListCache();
                clearIdCache(KEY);
            }
        });
    }



    @Override
    public List<BookVO> search(BookPageDTO dto) {
        int offset = (dto.getPage() - 1) * dto.getSize();
        return bookMapper.selectByCondition(dto.getTitle(), dto.getAuthor(),
                        dto.getIsbn(), dto.getCategoryId(), offset, dto.getSize())
                .stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long count(BookPageDTO dto) {
        return bookMapper.countByCondition(dto.getTitle(), dto.getAuthor(),
                dto.getIsbn(), dto.getCategoryId());
    }

    /** 清除列表缓存 */
    private void clearListCache() {
        try {
            redisTemplate.delete(BOOK_LIST_KEY);
        } catch (Exception ignored) {
            log.warn("缓存删除失败",ignored);
        }
    }

    private void clearIdCache(String KEY){
        try {
            redisTemplate.delete(KEY);
        }catch (Exception ignored) {
            log.warn("缓存删除失败",ignored);
        }
    }

    /** Entity → VO */
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

    /** DTO → Entity */
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
