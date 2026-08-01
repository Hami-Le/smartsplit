package com.smartsplit.expense.service;

import com.smartsplit.expense.dto.CategoryResponse;
import com.smartsplit.expense.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getIcon()
                ))
                .toList();
    }
}
