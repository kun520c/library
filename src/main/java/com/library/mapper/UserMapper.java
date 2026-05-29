package com.library.mapper;

import com.library.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO `User`(username, account, password) VALUES(#{username}, #{account}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void register(User user);

    @Select("SELECT id, username, account, password FROM `User` WHERE username = #{username}")
    User getUserByUsername(String username);

    @Select("SELECT id, username, account, password FROM `User` WHERE account = #{account}")
    User getUserByAccount(String account);
}
