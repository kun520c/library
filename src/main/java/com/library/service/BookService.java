package com.library.service;

import com.library.model.dto.BookDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.vo.BookVO;

import java.util.List;

/**
 * 图书 Service 接口
 */
public interface BookService {

    /** 查询所有图书 */
    List<BookVO> list();

    /** 根据ID查询图书 */
    BookVO getById(Integer id);

    /** 新增图书 */
    void add(BookDTO dto);

    /** 更新图书 */
    void update(Integer id, BookDTO dto);

    /** 逻辑删除图书 */
    void delete(Integer id);

    /** 分页条件查询图书 */
    List<BookVO> search(BookPageDTO dto);

    /** 分页条件查询图书总数 */
    Long count(BookPageDTO dto);
}
