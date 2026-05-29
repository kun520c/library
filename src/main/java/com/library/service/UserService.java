package com.library.service;

import com.library.model.entity.User;
import com.library.model.dto.RegisterDTO;

public interface UserService {
    User getUserByAccount(String account);
    void register(RegisterDTO dto);
}
