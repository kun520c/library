package com.library.model.vo;

import com.library.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {
    private Integer id;
    private String username;
    private String account;
    private Role role;
    private LocalDateTime createdAt;
}
