package com.library.service;

import com.library.exception.BusinessException;
import com.library.mapper.BookMapper;
import com.library.mapper.CategoryMapper;
import com.library.model.dto.CategoryDTO;
import com.library.model.entity.Category;
import com.library.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private BookMapper bookMapper;
    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryMapper, bookMapper);
    }

    @Test
    void addsUniqueTrimmedCategory() {
        CategoryDTO dto = new CategoryDTO("  Java  ");
        when(categoryMapper.existsByName("Java")).thenReturn(false);
        when(categoryMapper.insert(any())).thenReturn(1);

        service.add(dto);

        verify(categoryMapper).insert(org.mockito.ArgumentMatchers.argThat(category -> "Java".equals(category.getName())));
    }

    @Test
    void duplicateNameReturnsConflict() {
        when(categoryMapper.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> service.add(new CategoryDTO("Java")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoryMapper, never()).insert(any());
    }

    @Test
    void deleteRejectsCategoryUsedByActiveBooks() {
        when(categoryMapper.selectByIdForUpdate(2)).thenReturn(category());
        when(bookMapper.countActiveByCategoryId(2)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(2))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verify(categoryMapper, never()).deleteById(2);
    }

    @Test
    void listsOnlyMapperProvidedActiveCategories() {
        when(categoryMapper.selectAll()).thenReturn(List.of(category()));

        assertThat(service.list()).extracting("name").containsExactly("Java");
    }

    private Category category() {
        Category category = new Category();
        category.setId(2);
        category.setName("Java");
        return category;
    }
}
