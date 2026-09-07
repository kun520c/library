package com.library.service;

import com.library.model.dto.LoginDTO;
import com.library.model.dto.RegisterDTO;
import com.library.model.vo.LoginVO;
import com.library.model.vo.UserVO;

public interface UserService {
    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO getCurrentUser();
}
