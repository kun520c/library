package com.library.security;

import com.library.model.entity.Role;

public record AuthenticatedUser(Integer userId, String username, Role role) {
}
