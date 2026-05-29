package com.library.controller;

import com.library.exception.BusinessException;
import com.library.common.Result;
import com.library.model.entity.User;
import com.library.model.dto.LoginDTO;
import com.library.model.dto.RegisterDTO;
import com.library.service.UserService;
import com.library.utils.JwtUtils;
import com.library.utils.PasswordUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result login(@RequestBody @Validated LoginDTO dto) {
        User loginUser = userService.getUserByAccount(dto.getAccount());

        if(loginUser == null){
            throw new BusinessException(404,"用户不存在");
        }
        if(!PasswordUtils.matches(dto.getPassword(),loginUser.getPassword())){
            throw new BusinessException(401,"密码错误");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", loginUser.getId());
        claims.put("username", loginUser.getUsername());
        String token = jwtUtils.generateToken(claims);
        return Result.success(token);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result register(@RequestBody @Validated RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
}