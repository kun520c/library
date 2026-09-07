package com.library.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginDTO {
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, max = 32, message = "账号长度必须为4到32个字符")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "账号格式不正确")
    private String account;
    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72个字符")
    private String password;

    public LoginDTO(String account, String password) {
        setAccount(account);
        this.password = password;
    }

    public void setAccount(String account) {
        this.account = account == null ? null : account.trim();
    }
}
