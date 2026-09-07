package com.library.service.impl;

import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.BorrowRecordMapper;
import com.library.model.dto.BorrowPageDTO;
import com.library.model.entity.Book;
import com.library.model.entity.BorrowRecord;
import com.library.model.entity.BorrowStatus;
import com.library.model.entity.Role;
import com.library.model.vo.BorrowRecordVO;
import com.library.model.vo.PageVO;
import com.library.security.AuthenticatedUser;
import com.library.security.UserContext;
import com.library.service.BookCacheInvalidator;
import com.library.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {
    private static final int BORROW_DAYS = 30;

    private final BookMapper bookMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final BookCacheInvalidator cacheInvalidator;
    private final Clock clock;

    @Override
    @Transactional
    public BorrowRecordVO borrow(Integer bookId) {
        AuthenticatedUser user = UserContext.getRequiredUser();
        Book book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "图书不存在");
        }
        if (bookMapper.decrementStockIfAvailable(bookId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "图书库存不足");
        }

        LocalDateTime borrowTime = LocalDateTime.now(clock);
        BorrowRecord record = new BorrowRecord();
        record.setUserId(user.userId());
        record.setBookId(bookId);
        record.setBorrowTime(borrowTime);
        record.setDueTime(borrowTime.plusDays(BORROW_DAYS));
        record.setStatus(BorrowStatus.BORROWED);
        if (borrowRecordMapper.insert(record) != 1) {
            throw new IllegalStateException("借阅记录未写入数据库");
        }
        cacheInvalidator.evictAfterCommit(bookId);
        return BorrowRecordVO.builder()
                .recordId(record.getId())
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .userId(user.userId())
                .username(user.username())
                .borrowTime(record.getBorrowTime())
                .dueTime(record.getDueTime())
                .status(record.getStatus())
                .overdue(false)
                .build();
    }

    @Override
    @Transactional
    public void returnBook(Long recordId) {
        AuthenticatedUser user = UserContext.getRequiredUser();
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "借阅记录不存在");
        }
        if (user.role() != Role.ADMIN && !record.getUserId().equals(user.userId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "不能归还其他用户的借阅记录");
        }
        if (record.getStatus() == BorrowStatus.RETURNED
                || borrowRecordMapper.markReturned(recordId, LocalDateTime.now(clock)) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, "该借阅记录已经归还");
        }
        if (bookMapper.incrementStock(record.getBookId()) != 1) {
            throw new IllegalStateException("归还图书时恢复库存失败");
        }
        cacheInvalidator.evictAfterCommit(record.getBookId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<BorrowRecordVO> myRecords(BorrowPageDTO dto) {
        return page(UserContext.getRequiredUser().userId(), dto);
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<BorrowRecordVO> allRecords(BorrowPageDTO dto) {
        AuthenticatedUser user = UserContext.getRequiredUser();
        if (user.role() != Role.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "权限不足");
        }
        return page(null, dto);
    }

    private PageVO<BorrowRecordVO> page(Integer userId, BorrowPageDTO dto) {
        long offset = ((long) dto.getPage() - 1L) * dto.getSize();
        List<BorrowRecordVO> records = borrowRecordMapper.selectByCondition(
                userId, dto.getStatus(), dto.getBookTitle(), offset, dto.getSize());
        LocalDateTime now = LocalDateTime.now(clock);
        records.forEach(record -> record.setOverdue(record.getStatus() == BorrowStatus.BORROWED
                && record.getDueTime() != null
                && record.getDueTime().isBefore(now)));
        long total = borrowRecordMapper.countByCondition(userId, dto.getStatus(), dto.getBookTitle());
        return new PageVO<>(records, total, dto.getPage(), dto.getSize());
    }
}
