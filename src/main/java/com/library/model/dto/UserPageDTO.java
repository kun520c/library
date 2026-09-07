package com.library.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPageDTO {
    @Size(max = 50, message = "用户名查询条件不能超过50个字符")
    private String username;

    @Size(max = 32, message = "账号查询条件不能超过32个字符")
    private String account;

    @Min(value = 1, message = "页码最小为1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer size = 10;

    public UserPageDTO(String username, String account, Integer page, Integer size) {
        setUsername(username);
        setAccount(account);
        setPage(page);
        setSize(size);
    }

    public void setUsername(String username) {
        this.username = normalize(username);
    }

    public void setAccount(String account) {
        this.account = normalize(account);
    }

    public void setPage(Integer page) {
        this.page = page == null ? 1 : page;
    }

    public void setSize(Integer size) {
        this.size = size == null ? 10 : size;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
