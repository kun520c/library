package com.library.controller;

import com.library.config.WebConfig;
import com.library.config.properties.CorsProperties;
import com.library.exception.BusinessException;
import com.library.exception.GlobalExceptionHandler;
import com.library.model.entity.Role;
import com.library.security.AuthenticatedUser;
import com.library.interceptor.AuthenticationInterceptor;
import com.library.security.JwtService;
import com.library.service.BorrowService;
import com.library.service.CategoryService;
import com.library.service.BookService;
import com.library.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {BookController.class, UserController.class, CategoryController.class, BorrowController.class})
@Import({WebConfig.class, AuthenticationInterceptor.class, GlobalExceptionHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
class ApiSecurityWebTest {
    private static final String VALID_BOOK_JSON = """
            {"title":"Spring","author":"Author","isbn":"978-7-111","price":59.90,"stock":5,"categoryId":2}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private BorrowService borrowService;
    @MockitoBean
    private JwtService jwtService;

    @BeforeEach
    void configureTokens() {
        Claims userClaims = mock(Claims.class);
        Claims adminClaims = mock(Claims.class);
        when(jwtService.parseToken("user-token")).thenReturn(userClaims);
        when(jwtService.parseToken("admin-token")).thenReturn(adminClaims);
        when(jwtService.toPrincipal(userClaims)).thenReturn(new AuthenticatedUser(1, "reader", Role.USER));
        when(jwtService.toPrincipal(adminClaims)).thenReturn(new AuthenticatedUser(2, "admin", Role.ADMIN));
    }

    @Test
    void unauthenticatedProtectedRequestReturnsReal401() throws Exception {
        mockMvc.perform(get("/book"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCannotCreateBookAndGetsReal403() throws Exception {
        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BOOK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        verify(bookService, never()).add(any());
    }

    @Test
    void adminCanCreateBook() throws Exception {
        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BOOK_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(bookService).add(any());
    }

    @Test
    void malformedJsonReturns400WithMatchingCode() throws Exception {
        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void dtoValidationFailureReturns400() throws Exception {
        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" ","author":"Author","isbn":"x","price":-1,"stock":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void paginationLimitsReturn400() throws Exception {
        mockMvc.perform(get("/book?page=0&size=101").header("Authorization", "Bearer user-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void userCannotCreateCategoryButCanBorrow() throws Exception {
        mockMvc.perform(post("/category")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        mockMvc.perform(post("/borrow/1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk());
        verify(borrowService).borrow(1);
    }

    @Test
    void currentUserEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void invalidPathIdReturns400() throws Exception {
        mockMvc.perform(get("/book/0").header("Authorization", "Bearer user-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void missingBookReturns404() throws Exception {
        when(bookService.getById(99)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "图书不存在"));

        mockMvc.perform(get("/book/99").header("Authorization", "Bearer user-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void databaseUniqueConstraintReturns409() throws Exception {
        doThrow(new DuplicateKeyException("duplicate ISBN"))
                .when(bookService).add(any());

        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BOOK_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void nonUniqueDataIntegrityFailureIsSanitizedAs500() throws Exception {
        doThrow(new DataIntegrityViolationException("internal SQL details"))
                .when(bookService).add(any());

        mockMvc.perform(post("/book")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BOOK_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统内部错误"));
    }

    @Test
    void registrationCannotSupplyAdminRole() throws Exception {
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"account":"reader","password":"secret1","username":"Reader","role":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        verify(userService, never()).register(any());
    }

    @Test
    void nonBearerAndEmptyBearerHeadersReturn401() throws Exception {
        mockMvc.perform(get("/book").header("Authorization", "user-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/book").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userCannotUpdateBook() throws Exception {
        mockMvc.perform(put("/book/1")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BOOK_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
