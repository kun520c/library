package com.library.controller;

import com.library.common.Result;
import com.library.model.dto.BookCreateDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.dto.BookUpdateDTO;
import com.library.model.dto.StockAdjustmentDTO;
import com.library.model.entity.Role;
import com.library.model.vo.BookVO;
import com.library.model.vo.PageVO;
import com.library.security.RequireRole;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图书管理 Controller
 */
@Tag(name = "图书管理")
@RestController
@RequestMapping("/book")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "分页查询图书（已登录用户）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未认证")
    })
    @GetMapping
    public Result<PageVO<BookVO>> list(@Valid BookPageDTO dto) {
        return Result.success(bookService.page(dto));
    }

    @Operation(summary = "根据ID查询图书（已登录用户）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "ID非法"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "404", description = "图书不存在")
    })
    @GetMapping("/{id}")
    public Result<BookVO> getById(@PathVariable @Positive(message = "图书ID必须为正数") Integer id) {
        return Result.success(bookService.getById(id));
    }

    @Operation(summary = "新增图书（ADMIN）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "新增成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无 ADMIN 权限"),
            @ApiResponse(responseCode = "409", description = "ISBN 冲突")
    })
    @PostMapping
    @RequireRole(Role.ADMIN)
    public Result<Void> add(@RequestBody @Valid BookCreateDTO dto) {
        bookService.add(dto);
        return Result.success();
    }

    @Operation(summary = "更新图书（ADMIN）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无 ADMIN 权限"),
            @ApiResponse(responseCode = "404", description = "图书不存在"),
            @ApiResponse(responseCode = "409", description = "ISBN 冲突")
    })
    @PutMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> update(@PathVariable @Positive(message = "图书ID必须为正数") Integer id,
                               @RequestBody @Valid BookUpdateDTO dto) {
        bookService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "按增量调整图书库存（ADMIN）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "调整成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无 ADMIN 权限"),
            @ApiResponse(responseCode = "404", description = "图书不存在"),
            @ApiResponse(responseCode = "409", description = "库存调整超出允许范围")
    })
    @PatchMapping("/{id}/stock")
    @RequireRole(Role.ADMIN)
    public Result<Void> adjustStock(@PathVariable @Positive(message = "图书ID必须为正数") Integer id,
                                    @RequestBody @Valid StockAdjustmentDTO dto) {
        bookService.adjustStock(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除图书（ADMIN，逻辑删除）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "400", description = "ID非法"),
            @ApiResponse(responseCode = "401", description = "未认证"),
            @ApiResponse(responseCode = "403", description = "无 ADMIN 权限"),
            @ApiResponse(responseCode = "404", description = "图书不存在"),
            @ApiResponse(responseCode = "409", description = "图书仍有未归还借阅记录")
    })
    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> delete(@PathVariable @Positive(message = "图书ID必须为正数") Integer id) {
        bookService.delete(id);
        return Result.success();
    }
}
