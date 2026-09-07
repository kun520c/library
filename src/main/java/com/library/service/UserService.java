package com.library.service;

import com.library.model.dto.LoginDTO;
import com.library.model.dto.RegisterDTO;
import com.library.model.dto.UpdateProfileDTO;
import com.library.model.dto.UserPageDTO;
import com.library.model.vo.AdminUserVO;
import com.library.model.vo.LoginVO;
import com.library.model.vo.PageVO;
import com.library.model.vo.UserVO;

public interface UserService {
    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    UserVO getCurrentUser();

    PageVO<AdminUserVO> pageUsers(UserPageDTO dto);

    UserVO updateProfile(UpdateProfileDTO dto);
}
