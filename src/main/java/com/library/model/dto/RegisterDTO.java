package com.library.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterDTO {
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度必须为4到32个字符")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "账号只能包含字母、数字、点、下划线和连字符")
    private String account;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 72, message = "密码长度必须为6到72个字符")
    private String password;
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50个字符")
    private String username;

    public RegisterDTO(String account, String password, String username) {
        setAccount(account);
        this.password = password;
        setUsername(username);
    }

    public void setAccount(String account) {
        this.account = account == null ? null : account.trim();
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }
}
