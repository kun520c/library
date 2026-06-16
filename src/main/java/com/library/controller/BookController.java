package com.library.controller;

import com.library.common.Result;
import com.library.model.dto.BookDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.vo.BookVO;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图书管理 Controller
 */
@Tag(name = "图书管理")
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书")
    @GetMapping
    public Result list(@Validated BookPageDTO dto) {
        List<BookVO> search = bookService.search(dto);
        long count = bookService.count(dto);
        return Result.page(search, count);
    }

    @Operation(summary = "根据ID查询图书")
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        return Result.success(bookService.getById(id));
    }

    @Operation(summary = "新增图书")
    @PostMapping
    public Result add(@RequestBody @Validated BookDTO dto) {
        bookService.add(dto);
        return Result.success();
    }

    @Operation(summary = "更新图书")
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody @Validated BookDTO dto) {
        bookService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除图书（逻辑删除）")
    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        bookService.delete(id);
        return Result.success();
    }
}
