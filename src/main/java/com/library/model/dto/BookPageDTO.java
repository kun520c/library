package com.library.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书分页查询 DTO
 */
@Data
@NoArgsConstructor
public class BookPageDTO {
    @Size(max = 200, message = "书名查询条件不能超过200个字符")
    private String title;
    @Size(max = 100, message = "作者查询条件不能超过100个字符")
    private String author;
    @Size(max = 32, message = "ISBN查询条件不能超过32个字符")
    private String isbn;
    @Positive(message = "分类ID必须为正数")
    private Integer categoryId;
    /** 页码，默认第1页 */
    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;
    /** 每页条数，默认10 */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer size = 10;

    public BookPageDTO(String title, String author, String isbn, Integer categoryId, Integer page, Integer size) {
        setTitle(title);
        setAuthor(author);
        setIsbn(isbn);
        this.categoryId = categoryId;
        setPage(page);
        setSize(size);
    }

    public void setTitle(String title) {
        this.title = normalize(title);
    }

    public void setAuthor(String author) {
        this.author = normalize(author);
    }

    public void setIsbn(String isbn) {
        this.isbn = normalize(isbn);
    }

    public void setPage(Integer page) {
        this.page = page == null ? 1 : page;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 10 : size;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
