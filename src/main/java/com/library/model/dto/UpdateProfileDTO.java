package com.library.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateProfileDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名不能超过50个字符")
    private String username;

    public UpdateProfileDTO(String username) {
        setUsername(username);
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }
}
