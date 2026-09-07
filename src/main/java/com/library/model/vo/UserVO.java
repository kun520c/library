package com.library.model.vo;

import com.library.model.entity.Role;

public record UserVO(Integer id, String username, String account, Role role) {
}
