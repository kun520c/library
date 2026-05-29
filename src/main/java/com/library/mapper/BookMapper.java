package com.library.mapper;

import com.library.model.entity.Book;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图书 Mapper
 */
@Mapper
public interface BookMapper {

    /** 查询所有未删除的图书 */
    @Select("SELECT id, title, author, isbn, price, stock, category_id, is_deleted FROM Book WHERE is_deleted = 0")
    List<Book> selectAll();

    /** 根据ID查询未删除的图书 */
    @Select("SELECT id, title, author, isbn, price, stock, category_id, is_deleted FROM Book WHERE id = #{id} AND is_deleted = 0")
    Book selectById(@Param("id") Integer id);

    /** 新增图书 */
    @Insert("INSERT INTO Book(title, author, isbn, price, stock, category_id) VALUES(#{title}, #{author}, #{isbn}, #{price}, #{stock}, #{categoryId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Book book);

    /** 更新图书 */
    @Update("UPDATE Book SET title = #{title}, author = #{author}, isbn = #{isbn}, price = #{price}, stock = #{stock}, category_id = #{categoryId} WHERE id = #{id} AND is_deleted = 0")
    int update(Book book);

    /** 逻辑删除图书 */
    @Update("UPDATE Book SET is_deleted = 1 WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    /** 动态条件查询图书（分页） */
    List<Book> selectByCondition(@Param("title") String title,
                                 @Param("author") String author,
                                 @Param("isbn") String isbn,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    /** 动态条件查询总数 */
    Long countByCondition(@Param("title") String title,
                          @Param("author") String author,
                          @Param("isbn") String isbn,
                          @Param("categoryId") Integer categoryId);
}