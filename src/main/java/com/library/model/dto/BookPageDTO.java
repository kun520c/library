package com.library.model.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图书分页查询 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookPageDTO {
    private String title;
    private String author;
    private String isbn;
    private Integer categoryId;
    /** 页码，默认第1页 */
    @Min(1)
    private Integer page = 1;
    /** 每页条数，默认10 */
    @Min(1)
    private Integer size = 10;

    public boolean hasCondition() {
        return this.getTitle() != null || this.getAuthor() != null
                || this.getIsbn() != null || this.getCategoryId() != null;
    }
}