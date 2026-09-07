package com.library.service;

import com.library.model.dto.CategoryDTO;
import com.library.model.vo.CategoryVO;

import java.util.List;

public interface CategoryService {
    CategoryVO getById(Integer id);

    List<CategoryVO> list();

    void add(CategoryDTO dto);

    void update(Integer id, CategoryDTO dto);

    void delete(Integer id);
}
