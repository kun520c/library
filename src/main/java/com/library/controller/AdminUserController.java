package com.library.controller;

import com.library.common.Result;
import com.library.model.dto.UserPageDTO;
import com.library.model.entity.Role;
import com.library.model.vo.AdminUserVO;
import com.library.model.vo.PageVO;
import com.library.security.RequireRole;
import com.library.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员用户查询")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private final UserService userService;

    @Operation(summary = "分页查询用户（ADMIN）")
    @GetMapping
    @RequireRole(Role.ADMIN)
    public Result<PageVO<AdminUserVO>> list(@Valid UserPageDTO dto) {
        return Result.success(userService.pageUsers(dto));
    }
}
