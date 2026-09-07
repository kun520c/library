package com.library.service;

import com.library.model.dto.BookDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.vo.BookVO;
import com.library.model.vo.PageVO;

/**
 * 图书 Service 接口
 */
public interface BookService {

    /** 根据ID查询图书 */
    BookVO getById(Integer id);

    /** 新增图书 */
    void add(BookDTO dto);

    /** 更新图书 */
    void update(Integer id, BookDTO dto);

    /** 逻辑删除图书 */
    void delete(Integer id);

    /** 分页条件查询图书 */
    PageVO<BookVO> page(BookPageDTO dto);
}
