package com.library.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.Result;
import com.library.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;

    private final ObjectMapper objectMapper;
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");  //Authorization  请求头中获取

        if(token == null || token.isEmpty()){
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Result result = Result.error(401,"请先登录");
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
            return false;
        }

        try{
            jwtUtils.parseToken(token);
        }catch (Exception e){
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Result result = Result.error(401,"错误的令牌");
            String json = objectMapper.writeValueAsString(result);
            response.getWriter().write(json);
            return false;
        }
        return true;
    }
}
