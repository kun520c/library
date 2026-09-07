package com.library.mapper;

import com.library.model.entity.Book;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 图书 Mapper
 */
@Mapper
public interface BookMapper {

    /** 根据ID查询未删除的图书 */
    @Select("SELECT id, title, author, isbn, price, stock, category_id, is_deleted, created_at, updated_at " +
            "FROM books WHERE id = #{id} AND is_deleted = 0")
    Book selectById(@Param("id") Integer id);

    /** 锁定未删除图书，协调借阅、归还与删除操作 */
    @Select("SELECT id, title, author, isbn, price, stock, category_id, is_deleted, created_at, updated_at " +
            "FROM books WHERE id = #{id} AND is_deleted = 0 FOR UPDATE")
    Book selectByIdForUpdate(@Param("id") Integer id);

    /** 新增图书 */
    @Insert("INSERT INTO books(title, author, isbn, price, stock, category_id) " +
            "VALUES(#{title}, #{author}, #{isbn}, #{price}, #{stock}, #{categoryId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Book book);

    /** 只更新图书基本信息，库存由原子增减接口维护 */
    @Update("UPDATE books SET title = #{title}, author = #{author}, isbn = #{isbn}, price = #{price}, " +
            "category_id = #{categoryId} WHERE id = #{id} AND is_deleted = 0")
    int updateBasicInfo(Book book);

    /** 逻辑删除图书 */
    @Update("UPDATE books SET is_deleted = 1 WHERE id = #{id} AND is_deleted = 0")
    int deleteById(@Param("id") Integer id);

    @Select("SELECT EXISTS(SELECT 1 FROM books WHERE active_isbn = #{isbn})")
    boolean existsByIsbn(@Param("isbn") String isbn);

    @Select("SELECT EXISTS(SELECT 1 FROM books WHERE active_isbn = #{isbn} AND id <> #{id})")
    boolean existsByIsbnAndIdNot(@Param("isbn") String isbn, @Param("id") Integer id);

    @Select("SELECT COUNT(*) FROM books WHERE category_id = #{categoryId} AND is_deleted = 0")
    long countActiveByCategoryId(@Param("categoryId") Integer categoryId);

    @Update("UPDATE books SET stock = stock - 1 WHERE id = #{id} AND stock > 0 AND is_deleted = 0")
    int decrementStockIfAvailable(@Param("id") Integer id);

    @Update("UPDATE books SET stock = stock + 1 WHERE id = #{id}")
    int incrementStock(@Param("id") Integer id);

    // DECIMAL 避免 MySQL UNSIGNED 在下界检查前发生负数溢出，也避免整数加法溢出。
    @Update("UPDATE books SET stock = CAST(stock AS DECIMAL(20, 0)) + #{delta} " +
            "WHERE id = #{id} AND is_deleted = 0 " +
            "AND CAST(stock AS DECIMAL(20, 0)) + #{delta} BETWEEN 0 AND 2147483647")
    int adjustStock(@Param("id") Integer id, @Param("delta") Integer delta);

    /** 动态条件查询图书（分页） */
    List<Book> selectByCondition(@Param("title") String title,
                                 @Param("author") String author,
                                 @Param("isbn") String isbn,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("offset") Long offset,
                                 @Param("limit") Integer limit);

    /** 动态条件查询总数 */
    long countByCondition(@Param("title") String title,
                          @Param("author") String author,
                          @Param("isbn") String isbn,
                          @Param("categoryId") Integer categoryId);
}
