package com.library.mapper;

import com.library.model.entity.BorrowRecord;
import com.library.model.entity.BorrowStatus;
import com.library.model.vo.BorrowRecordVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowRecordMapper {
    @Select("SELECT id, user_id, book_id, borrow_time, due_time, return_time, status, created_at, updated_at " +
            "FROM borrow_records WHERE id = #{id}")
    BorrowRecord selectById(@Param("id") Long id);

    @Insert("INSERT INTO borrow_records(user_id, book_id, borrow_time, due_time, status) " +
            "VALUES(#{userId}, #{bookId}, #{borrowTime}, #{dueTime}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BorrowRecord record);

    @Update("UPDATE borrow_records SET status = 'RETURNED', return_time = #{returnTime} " +
            "WHERE id = #{id} AND status = 'BORROWED'")
    int markReturned(@Param("id") Long id, @Param("returnTime") LocalDateTime returnTime);

    List<BorrowRecordVO> selectByCondition(@Param("userId") Integer userId,
                                           @Param("status") BorrowStatus status,
                                           @Param("bookTitle") String bookTitle,
                                           @Param("offset") Long offset,
                                           @Param("limit") Integer limit);

    long countByCondition(@Param("userId") Integer userId,
                          @Param("status") BorrowStatus status,
                          @Param("bookTitle") String bookTitle);
}
