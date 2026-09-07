package com.library.mapper;

import com.library.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO users(username, account, password, role) VALUES(#{username}, #{account}, #{password}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT id, username, account, password, role FROM users WHERE account = #{account}")
    User findByAccount(@Param("account") String account);

    @Select("SELECT id, username, account, role FROM users WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("SELECT EXISTS(SELECT 1 FROM users WHERE account = #{account})")
    boolean existsByAccount(@Param("account") String account);
}
