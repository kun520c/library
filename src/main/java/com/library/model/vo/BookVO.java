package com.library.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 图书出参 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookVO {
    private Integer id;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
    private Integer stock;
    private Integer categoryId;
}