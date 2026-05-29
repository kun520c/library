package com.library;

import com.library.model.dto.RegisterDTO;
import com.library.model.entity.User;
import com.library.service.UserService;
import com.library.utils.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LibrarySystemApplicationTests {
    @Autowired
    private UserService userService;
    @Test
    void register() {
        RegisterDTO dto = new RegisterDTO("2725595357","123456","坤哥");
        userService.register(dto);

        User user = userService.getUserByAccount("2725595357");

        assertNotNull(user);
        assertEquals("2725595357",user.getAccount());
        assertEquals("坤哥",user.getUsername());
        assertTrue(PasswordUtils.matches("123456",user.getPassword()));
    }

}
