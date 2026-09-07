package com.library.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Result;
import com.library.model.entity.Role;
import com.library.security.AuthenticatedUser;
import com.library.security.JwtService;
import com.library.security.RequireRole;
import com.library.security.UserContext;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthenticationInterceptor implements HandlerInterceptor {
    private static final String BEARER = "Bearer";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UserContext.clear();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractBearerToken(request.getHeader("Authorization"));
        if (token == null) {
            writeError(response, HttpStatus.UNAUTHORIZED, "认证失败，请提供有效的Bearer Token");
            return false;
        }

        try {
            AuthenticatedUser user = jwtService.toPrincipal(jwtService.parseToken(token));
            UserContext.set(user);
            RequireRole requirement = findRoleRequirement(handler);
            if (requirement != null && user.role() != requirement.value()) {
                UserContext.clear();
                writeError(response, HttpStatus.FORBIDDEN, "权限不足");
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            UserContext.clear();
            writeError(response, HttpStatus.UNAUTHORIZED, "认证失败，请提供有效的Bearer Token");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        UserContext.clear();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        int separator = authorization.indexOf(' ');
        if (separator <= 0 || !BEARER.equalsIgnoreCase(authorization.substring(0, separator))) {
            return null;
        }
        String token = authorization.substring(separator + 1).trim();
        return token.isEmpty() || token.indexOf(' ') >= 0 ? null : token;
    }

    private RequireRole findRoleRequirement(Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return null;
        }
        RequireRole requirement = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireRole.class);
        return requirement != null
                ? requirement
                : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireRole.class);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(status, message));
    }
}
