package com.library.service.impl;

import com.library.mapper.UserMapper;
import com.library.model.entity.User;
import com.library.model.dto.RegisterDTO;
import com.library.service.UserService;
import com.library.utils.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User getUserByAccount(String account){
        return userMapper.getUserByAccount(account);
    }

    @Transactional
    @Override
    public void register(RegisterDTO dto){
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setAccount(dto.getAccount());
        user.setPassword(PasswordUtils.encode(dto.getPassword()));
        userMapper.register(user);
        log.info("用户注册成功, username={}, account={}", dto.getUsername(), dto.getAccount());
    }
}