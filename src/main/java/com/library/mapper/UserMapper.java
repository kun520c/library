package com.library.mapper;

import com.library.model.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO `User`(username, account, password) VALUES(#{username}, #{account}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void register(User user);

    @Select("SELECT id, username, account, password FROM `User` WHERE account = #{account}")
    User getUserByAccount(@Param("account") String account);
}
