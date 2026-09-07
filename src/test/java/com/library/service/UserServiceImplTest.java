package com.library.service;

import com.library.exception.BusinessException;
import com.library.mapper.UserMapper;
import com.library.model.dto.LoginDTO;
import com.library.model.dto.RegisterDTO;
import com.library.model.dto.UpdateProfileDTO;
import com.library.model.dto.UserPageDTO;
import com.library.model.entity.Role;
import com.library.model.entity.User;
import com.library.model.vo.AdminUserVO;
import com.library.security.JwtService;
import com.library.security.AuthenticatedUser;
import com.library.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import com.library.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtService jwtService;
    private BCryptPasswordEncoder passwordEncoder;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new UserServiceImpl(userMapper, passwordEncoder, jwtService);
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    @Test
    void registersUserWithBcryptAndUserRole() {
        RegisterDTO dto = new RegisterDTO(" reader ", "secret1", " Reader ");
        when(userMapper.existsByAccount("reader")).thenReturn(false);
        when(userMapper.insert(org.mockito.ArgumentMatchers.any())).thenReturn(1);

        service.register(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getAccount()).isEqualTo("reader");
        assertThat(saved.getUsername()).isEqualTo("Reader");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(passwordEncoder.matches("secret1", saved.getPassword())).isTrue();
    }

    @Test
    void duplicateAccountReturnsConflict() {
        RegisterDTO dto = new RegisterDTO("reader", "secret1", "Reader");
        when(userMapper.existsByAccount("reader")).thenReturn(true);

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(userMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loginReturnsBearerResponse() {
        User user = user("reader", passwordEncoder.encode("secret1"));
        when(userMapper.findByAccount("reader")).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.expiresInSeconds()).thenReturn(43_200L);

        var response = service.login(new LoginDTO(" reader ", "secret1"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(43_200L);
    }

    @Test
    void missingAccountAndWrongPasswordUseSameUnauthorizedMessage() {
        when(userMapper.findByAccount("none")).thenReturn(null);
        when(userMapper.findByAccount("reader")).thenReturn(user("reader", passwordEncoder.encode("correct")));

        BusinessException missing = catchLoginException(new LoginDTO("none", "anything"));
        BusinessException wrong = catchLoginException(new LoginDTO("reader", "wrong"));

        assertThat(missing.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrong.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(missing.getMessage()).isEqualTo("账号或密码错误").isEqualTo(wrong.getMessage());
    }

    @Test
    void currentUserReturnsSafeViewWithoutPassword() {
        UserContext.set(new AuthenticatedUser(1, "Reader", Role.USER));
        when(userMapper.findById(1)).thenReturn(user("reader", "secret-hash"));

        var response = service.getCurrentUser();

        assertThat(response.id()).isEqualTo(1);
        assertThat(response.account()).isEqualTo("reader");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.toString()).doesNotContain("secret-hash");
    }

    @Test
    void adminCanPageUsersWithNormalizedFilters() {
        UserContext.set(new AuthenticatedUser(10, "Admin", Role.ADMIN));
        UserPageDTO query = new UserPageDTO(" Reader ", " read ", 2, 5);
        AdminUserVO row = new AdminUserVO(1, "Reader", "reader", Role.USER, LocalDateTime.now());
        when(userMapper.selectByCondition("Reader", "read", 5L, 5)).thenReturn(List.of(row));
        when(userMapper.countByCondition("Reader", "read")).thenReturn(6L);

        var page = service.pageUsers(query);

        assertThat(page.getList()).containsExactly(row);
        assertThat(page.getTotal()).isEqualTo(6);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(5);
    }

    @Test
    void nonAdminCannotPageUsers() {
        UserContext.set(new AuthenticatedUser(1, "Reader", Role.USER));

        assertThatThrownBy(() -> service.pageUsers(new UserPageDTO()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(userMapper, never()).selectByCondition(any(), any(), any(), any());
    }

    @Test
    void updatesOnlyCurrentUsernameAndReturnsUpdatedView() {
        UserContext.set(new AuthenticatedUser(1, "Old claim", Role.USER));
        User current = user("reader", "secret-hash");
        when(userMapper.findById(1)).thenReturn(current);
        when(userMapper.updateUsername(1, "New Name")).thenReturn(1);

        var response = service.updateProfile(new UpdateProfileDTO("  New Name  "));

        assertThat(response.username()).isEqualTo("New Name");
        assertThat(response.account()).isEqualTo("reader");
        assertThat(response.role()).isEqualTo(Role.USER);
        verify(userMapper).updateUsername(1, "New Name");
    }

    private BusinessException catchLoginException(LoginDTO dto) {
        try {
            service.login(dto);
            throw new AssertionError("expected BusinessException");
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private User user(String account, String password) {
        User user = new User();
        user.setId(1);
        user.setAccount(account);
        user.setUsername("Reader");
        user.setPassword(password);
        user.setRole(Role.USER);
        return user;
    }
}
