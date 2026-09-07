package com.library.service;

import com.library.model.dto.BorrowPageDTO;
import com.library.model.vo.BorrowRecordVO;
import com.library.model.vo.PageVO;

public interface BorrowService {
    BorrowRecordVO borrow(Integer bookId);

    void returnBook(Long recordId);

    BorrowRecordVO getById(Long recordId);

    PageVO<BorrowRecordVO> myRecords(BorrowPageDTO dto);

    PageVO<BorrowRecordVO> allRecords(BorrowPageDTO dto);
}
