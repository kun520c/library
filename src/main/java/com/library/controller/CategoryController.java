package com.library.controller;

import com.library.common.Result;
import com.library.model.dto.CategoryDTO;
import com.library.model.entity.Role;
import com.library.model.vo.CategoryVO;
import com.library.security.RequireRole;
import com.library.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "图书分类")
@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "查询分类列表")
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.success(categoryService.list());
    }

    @Operation(summary = "根据ID查询分类")
    @GetMapping("/{id}")
    public Result<CategoryVO> getById(@PathVariable @Positive(message = "分类ID必须为正数") Integer id) {
        return Result.success(categoryService.getById(id));
    }

    @Operation(summary = "新增分类（ADMIN）")
    @PostMapping
    @RequireRole(Role.ADMIN)
    public Result<Void> add(@RequestBody @Valid CategoryDTO dto) {
        categoryService.add(dto);
        return Result.success();
    }

    @Operation(summary = "更新分类（ADMIN）")
    @PutMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> update(@PathVariable @Positive(message = "分类ID必须为正数") Integer id,
                               @RequestBody @Valid CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.success();
    }

    @Operation(summary = "删除分类（ADMIN，逻辑删除）")
    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public Result<Void> delete(@PathVariable @Positive(message = "分类ID必须为正数") Integer id) {
        categoryService.delete(id);
        return Result.success();
    }
}
