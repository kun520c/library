package com.library.controller;

import com.library.common.Result;
import com.library.model.dto.BorrowPageDTO;
import com.library.model.entity.Role;
import com.library.model.vo.BorrowRecordVO;
import com.library.model.vo.PageVO;
import com.library.security.RequireRole;
import com.library.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "图书借阅")
@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class BorrowController {
    private final BorrowService borrowService;

    @Operation(summary = "借书")
    @PostMapping("/borrow/{bookId}")
    public Result<BorrowRecordVO> borrow(@PathVariable @Positive(message = "图书ID必须为正数") Integer bookId) {
        return Result.success(borrowService.borrow(bookId));
    }

    @Operation(summary = "归还图书")
    @PostMapping("/borrow/{recordId}/return")
    public Result<Void> returnBook(@PathVariable @Positive(message = "借阅记录ID必须为正数") Long recordId) {
        borrowService.returnBook(recordId);
        return Result.success();
    }

    @Operation(summary = "分页查询我的借阅记录")
    @GetMapping("/borrow/my")
    public Result<PageVO<BorrowRecordVO>> myRecords(@Valid BorrowPageDTO dto) {
        return Result.success(borrowService.myRecords(dto));
    }

    @Operation(summary = "分页查询全部借阅记录（ADMIN）")
    @GetMapping("/admin/borrow")
    @RequireRole(Role.ADMIN)
    public Result<PageVO<BorrowRecordVO>> allRecords(@Valid BorrowPageDTO dto) {
        return Result.success(borrowService.allRecords(dto));
    }
}
