package com.library.security;

import com.library.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class UserContext {
    private static final ThreadLocal<AuthenticatedUser> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static AuthenticatedUser getRequiredUser() {
        AuthenticatedUser user = CURRENT_USER.get();
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
        return user;
    }

    public static void set(AuthenticatedUser user) {
        CURRENT_USER.set(user);
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
