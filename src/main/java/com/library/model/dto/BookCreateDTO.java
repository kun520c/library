package com.library.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class BookCreateDTO {
    @NotBlank(message = "书名不能为空")
    @Size(max = 200, message = "书名长度不能超过200个字符")
    private String title;

    @NotBlank(message = "作者不能为空")
    @Size(max = 100, message = "作者长度不能超过100个字符")
    private String author;

    @NotBlank(message = "ISBN不能为空")
    @Size(max = 32, message = "ISBN长度不能超过32个字符")
    private String isbn;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    @Digits(integer = 10, fraction = 2, message = "价格最多10位整数和2位小数")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @PositiveOrZero(message = "库存不能小于0")
    private Integer stock;

    @Positive(message = "分类ID必须为正数")
    private Integer categoryId;

    public BookCreateDTO(String title, String author, String isbn, BigDecimal price,
                         Integer stock, Integer categoryId) {
        setTitle(title);
        setAuthor(author);
        setIsbn(isbn);
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
