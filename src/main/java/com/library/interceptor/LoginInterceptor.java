package com.library.interceptor;

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
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = request.getHeader("Authorization");  //Authorization  请求头中获取

        if(token == null || token.isEmpty()){
            response.setStatus(401);
            return false;
        }

        try{
            jwtUtils.parseToken(token);
        }catch (Exception e){
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
