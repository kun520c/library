package com.library.service.impl;

import com.library.exception.BusinessException;
import com.library.mapper.UserMapper;
import com.library.model.dto.LoginDTO;
import com.library.model.dto.RegisterDTO;
import com.library.model.entity.Role;
import com.library.model.entity.User;
import com.library.model.vo.LoginVO;
import com.library.model.vo.UserVO;
import com.library.security.JwtService;
import com.library.security.UserContext;
import com.library.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String INVALID_CREDENTIALS = "账号或密码错误";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void register(RegisterDTO dto) {
        if (userMapper.existsByAccount(dto.getAccount())) {
            throw new BusinessException(HttpStatus.CONFLICT, "账号已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setAccount(dto.getAccount());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.USER);
        if (userMapper.insert(user) != 1) {
            throw new IllegalStateException("注册用户未写入数据库");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.findByAccount(dto.getAccount());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        String token = jwtService.generateToken(user);
        return new LoginVO("Bearer", token, jwtService.expiresInSeconds());
    }

    @Override
    @Transactional(readOnly = true)
    public UserVO getCurrentUser() {
        User user = userMapper.findById(UserContext.getRequiredUser().userId());
        if (user == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "用户不存在");
        }
        return new UserVO(user.getId(), user.getUsername(), user.getAccount(), user.getRole());
    }
}
