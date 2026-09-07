package com.library.service.impl;

import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import com.library.model.dto.CategoryDTO;
import com.library.model.entity.Category;
import com.library.model.vo.CategoryVO;
import com.library.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    @Override
    @Transactional(readOnly = true)
    public CategoryVO getById(Integer id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw notFound();
        }
        return toVO(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryVO> list() {
        return categoryMapper.selectAll().stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public void add(CategoryDTO dto) {
        if (categoryMapper.existsByName(dto.getName())) {
            throw nameConflict();
        }
        Category category = new Category();
        category.setName(dto.getName());
        if (categoryMapper.insert(category) != 1) {
            throw new IllegalStateException("新增分类未写入数据库");
        }
    }

    @Override
    @Transactional
    public void update(Integer id, CategoryDTO dto) {
        Category category = categoryMapper.selectByIdForUpdate(id);
        if (category == null) {
            throw notFound();
        }
        if (categoryMapper.existsByNameAndIdNot(dto.getName(), id)) {
            throw nameConflict();
        }
        category.setName(dto.getName());
        if (categoryMapper.update(category) != 1) {
            throw notFound();
        }
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        if (categoryMapper.selectByIdForUpdate(id) == null) {
            throw notFound();
        }
        if (bookMapper.countActiveByCategoryId(id) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "分类仍被有效图书使用，不能删除");
        }
        if (categoryMapper.deleteById(id) != 1) {
            throw notFound();
        }
    }

    private CategoryVO toVO(Category category) {
        return new CategoryVO(category.getId(), category.getName(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, "分类不存在");
    }

    private BusinessException nameConflict() {
        return new BusinessException(HttpStatus.CONFLICT, "分类名称已存在");
    }
}
