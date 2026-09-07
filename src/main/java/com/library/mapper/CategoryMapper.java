package com.library.mapper;

import com.library.model.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Select("SELECT id, name, is_deleted, created_at, updated_at FROM categories " +
            "WHERE id = #{id} AND is_deleted = 0")
    Category selectById(@Param("id") Integer id);

    @Select("SELECT id, name, is_deleted, created_at, updated_at FROM categories " +
            "WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Category selectByIdForUpdate(@Param("id") Integer id);

    @Select("SELECT id, name, is_deleted, created_at, updated_at FROM categories " +
            "WHERE is_deleted = 0 ORDER BY name, id")
    List<Category> selectAll();

    @Select("SELECT EXISTS(SELECT 1 FROM categories WHERE active_name = #{name})")
    boolean existsByName(@Param("name") String name);

    @Select("SELECT EXISTS(SELECT 1 FROM categories " +
            "WHERE active_name = #{name} AND id <> #{id})")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Integer id);

    @Insert("INSERT INTO categories(name) VALUES(#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Update("UPDATE categories SET name = #{name} WHERE id = #{id} AND is_deleted = 0")
    int update(Category category);

    @Update("UPDATE categories SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int deleteById(@Param("id") Integer id);
}
