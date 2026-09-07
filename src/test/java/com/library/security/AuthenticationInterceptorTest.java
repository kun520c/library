package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.interceptor.AuthenticationInterceptor;
import com.library.model.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationInterceptorTest {
    @Mock
    private JwtService jwtService;

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void bearerTokenPopulatesAndCompletionClearsContext() throws Exception {
        Claims claims = mock(Claims.class);
        AuthenticatedUser user = new AuthenticatedUser(1, "reader", Role.USER);
        when(jwtService.parseToken("valid-token")).thenReturn(claims);
        when(jwtService.toPrincipal(claims)).thenReturn(user);
        AuthenticationInterceptor interceptor = interceptor();
        MockHttpServletRequest request = request("GET", "Bearer valid-token");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(UserContext.getRequiredUser()).isEqualTo(user);

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertThatThrownByNoUser();
    }

    @Test
    void missingMalformedAndEmptyBearerTokensReturn401() throws Exception {
        for (String authorization : new String[]{null, "token", "Bearer ", "Bearer one two"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertThat(interceptor().preHandle(request("GET", authorization), response, new Object())).isFalse();
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString()).contains("\"code\":401");
        }
        verify(jwtService, never()).parseToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        when(jwtService.parseToken("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor().preHandle(request("GET", "Bearer expired"), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void optionsBypassesAuthentication() throws Exception {
        MockHttpServletRequest request = request("OPTIONS", "not-bearer");

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        verify(jwtService, never()).parseToken(org.mockito.ArgumentMatchers.anyString());
    }

    private AuthenticationInterceptor interceptor() {
        return new AuthenticationInterceptor(jwtService, new ObjectMapper());
    }

    private MockHttpServletRequest request(String method, String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/book");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    private void assertThatThrownByNoUser() {
        org.assertj.core.api.Assertions.assertThatThrownBy(UserContext::getRequiredUser)
                .isInstanceOf(com.library.exception.BusinessException.class);
    }
}
