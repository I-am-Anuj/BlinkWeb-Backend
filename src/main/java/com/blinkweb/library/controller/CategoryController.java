package com.blinkweb.library.controller;

import com.blinkweb.library.context.TenantContext;
import com.blinkweb.library.dto.ApiResponse;
import com.blinkweb.library.entity.Category;
import com.blinkweb.library.exception.ResourceNotFoundException;
import com.blinkweb.library.repository.CategoryRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Transactional
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public record CategoryRequest(@NotBlank String name, String description) {}

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        String tenantId = TenantContext.getTenantId();
        List<Category> categories = categoryRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved", tenantId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody CategoryRequest request) {
        String tenantId = TenantContext.getTenantId();
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Category with name '" + request.name() + "' already exists.");
        }
        Category category = new Category(request.name().trim(), request.description());
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(saved, "Category created successfully", tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getCategoryById(@PathVariable Long id) {
        String tenantId = TenantContext.getTenantId();
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved", tenantId));
    }
}
