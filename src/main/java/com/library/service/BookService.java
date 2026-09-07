package com.library.service;

import com.library.model.dto.BookCreateDTO;
import com.library.model.dto.BookPageDTO;
import com.library.model.dto.BookUpdateDTO;
import com.library.model.dto.StockAdjustmentDTO;
import com.library.model.vo.BookVO;
import com.library.model.vo.PageVO;

/**
 * 图书 Service 接口
 */
public interface BookService {

    /** 根据ID查询图书 */
    BookVO getById(Integer id);

    /** 新增图书 */
    void add(BookCreateDTO dto);

    /** 更新图书 */
    void update(Integer id, BookUpdateDTO dto);

    /** 按增量原子调整可用库存 */
    void adjustStock(Integer id, StockAdjustmentDTO dto);

    /** 逻辑删除图书 */
    void delete(Integer id);

    /** 分页条件查询图书 */
    PageVO<BookVO> page(BookPageDTO dto);
}
