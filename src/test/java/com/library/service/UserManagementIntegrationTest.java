package com.library.service;

import com.library.model.dto.UpdateProfileDTO;
import com.library.model.dto.UserPageDTO;
import com.library.model.entity.Role;
import com.library.security.AuthenticatedUser;
import com.library.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class UserManagementIntegrationTest {
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        jdbcTemplate.update("DELETE FROM borrow_records");
        jdbcTemplate.update("DELETE FROM books");
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("INSERT INTO users(id, username, account, password, role) " +
                "VALUES(1, 'Reader', 'reader01', 'hash-1', 'USER')");
        jdbcTemplate.update("INSERT INTO users(id, username, account, password, role) " +
                "VALUES(2, 'Alice', 'alice-reader', 'hash-2', 'USER')");
        jdbcTemplate.update("INSERT INTO users(id, username, account, password, role) " +
                "VALUES(3, 'Alicia', 'team-read', 'hash-3', 'USER')");
        jdbcTemplate.update("INSERT INTO users(id, username, account, password, role) " +
                "VALUES(4, 'Admin', 'admin01', 'hash-4', 'ADMIN')");
    }

    @AfterEach
    void clearContext() {
        UserContext.clear();
    }

    @Test
    void adminUserPagesUseStableDescendingIdOrder() {
        authenticateAdmin();

        var firstPage = userService.pageUsers(new UserPageDTO(null, null, 1, 2));
        var secondPage = userService.pageUsers(new UserPageDTO(null, null, 2, 2));

        assertThat(firstPage.getList()).extracting("id").containsExactly(4, 3);
        assertThat(secondPage.getList()).extracting("id").containsExactly(2, 1);
        assertThat(firstPage.getTotal()).isEqualTo(4);
    }

    @Test
    void adminUserPageSupportsUsernameAndAccountFilters() {
        authenticateAdmin();

        var usernamePage = userService.pageUsers(new UserPageDTO("Ali", null, 1, 10));
        var accountPage = userService.pageUsers(new UserPageDTO(null, "read", 1, 10));

        assertThat(usernamePage.getList()).extracting("username").containsExactly("Alicia", "Alice");
        assertThat(usernamePage.getTotal()).isEqualTo(2);
        assertThat(accountPage.getList()).extracting("account")
                .containsExactly("team-read", "alice-reader", "reader01");
        assertThat(accountPage.getTotal()).isEqualTo(3);
    }

    @Test
    void updatedUsernameIsImmediatelyReturnedByCurrentUserLookup() {
        UserContext.set(new AuthenticatedUser(1, "stale-token-name", Role.USER));

        var updated = userService.updateProfile(new UpdateProfileDTO("  Renamed Reader  "));
        var current = userService.getCurrentUser();

        assertThat(updated.username()).isEqualTo("Renamed Reader");
        assertThat(current.username()).isEqualTo("Renamed Reader");
        assertThat(current.account()).isEqualTo("reader01");
        assertThat(current.role()).isEqualTo(Role.USER);
        assertThat(jdbcTemplate.queryForObject("SELECT password FROM users WHERE id = 1", String.class))
                .isEqualTo("hash-1");
    }

    private void authenticateAdmin() {
        UserContext.set(new AuthenticatedUser(4, "Admin", Role.ADMIN));
    }
}
