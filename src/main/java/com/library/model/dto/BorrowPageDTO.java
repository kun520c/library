package com.library.model.dto;

import com.library.model.entity.BorrowStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BorrowPageDTO {
    private BorrowStatus status;

    @Size(max = 200, message = "书名查询条件不能超过200个字符")
    private String bookTitle;

    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer size = 10;

    public BorrowPageDTO(BorrowStatus status, String bookTitle, Integer page, Integer size) {
        this.status = status;
        setBookTitle(bookTitle);
        setPage(page);
        setSize(size);
    }

    public void setBookTitle(String bookTitle) {
        if (bookTitle == null || bookTitle.trim().isEmpty()) {
            this.bookTitle = null;
        } else {
            this.bookTitle = bookTitle.trim();
        }
    }

    public void setPage(Integer page) {
        this.page = page == null ? 1 : page;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 10 : size;
    }
}
