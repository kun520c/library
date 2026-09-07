package com.library.model.vo;

import com.library.model.entity.BorrowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordVO {
    private Long recordId;
    private Integer bookId;
    private String bookTitle;
    private Integer userId;
    private String username;
    private LocalDateTime borrowTime;
    private LocalDateTime dueTime;
    private LocalDateTime returnTime;
    private BorrowStatus status;
    private boolean overdue;
}
