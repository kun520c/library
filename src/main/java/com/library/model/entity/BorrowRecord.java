package com.library.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BorrowRecord {
    private Long id;
    private Integer userId;
    private Integer bookId;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private BorrowStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
