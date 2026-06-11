package com.docflow.ai.ticket.service;

import com.docflow.ai.ticket.dto.CategoryResponse;
import com.docflow.ai.ticket.dto.CreateCategoryRequest;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> listCategories(Long userId);
    CategoryResponse createCategory(Long userId, CreateCategoryRequest request);
    CategoryResponse updateCategory(Long id, Long userId, CreateCategoryRequest request);
    void deleteCategory(Long id, Long userId);
}
