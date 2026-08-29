package com.example.portfolio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.portfolio.dto.CategoryCreateDto;
import com.example.portfolio.dto.CategoryDto;
import com.example.portfolio.dto.CategoryUpdateDto;
import com.example.portfolio.dto.SubCategoryCreateDto;
import com.example.portfolio.dto.SubCategoryDto;
import com.example.portfolio.dto.SubCategoryUpdateDto;
import com.example.portfolio.model.Category;
import com.example.portfolio.service.CategoryService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // 전체 카테고리
    @GetMapping
    public List<CategoryDto> list(@RequestParam(name = "view", required = false) String view) {
        if ("main".equals(view)) {
            return categoryService.getCategoriesWithProjects();
        }
        return categoryService.getAllCategories();
    }

    // 생성
    @PostMapping
    public CategoryDto create(@RequestBody CategoryCreateDto dto) {
        return categoryService.createCategories(dto);
    }

    // 삭제
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }

    // 사용 여부
    @GetMapping("/{id}/used")
    public boolean isUsed(@PathVariable Long id) {
        return categoryService.isCategoryUsed(id);
    }

    // (기존) 엔티티 반환하는 API - 가능하면 제거 권장
    @GetMapping("/raw")
    public List<Category> raw() {
        return categoryService.getCategory();
    }

    // 수정
    @PutMapping("/{id}")
    public void update(@PathVariable Long id, @RequestBody CategoryUpdateDto dto) {
        // record 전환 시: categoryService.updateCategory(id, dto) 로 바꾸는게 정석
        categoryService.updateCategory(id, dto);
    }

    // 서브카테고리 생성
    @PostMapping("/{categoryId}/subcategories")
    public SubCategoryDto createSubCategory(
            @PathVariable Long categoryId,
            @RequestBody SubCategoryCreateDto dto
    ) {
        return categoryService.createSubCategory(categoryId, dto);
    }

    // 특정 카테고리의 서브카테고리 조회
    @GetMapping("/{categoryId}/subcategories")
    public List<SubCategoryDto> listSubCategories(@PathVariable Long categoryId) {
        return categoryService.getSubCategoriesWithProjects(categoryId);
    }

    // 서브카테고리 삭제
    @DeleteMapping("/subcategories/{subCategoryId}")
    public void deleteSubCategory(@PathVariable Long subCategoryId) {
        categoryService.deleteSubCategory(subCategoryId);
    }

    // 서브카테고리 사용 여부
    @GetMapping("/subcategories/{subCategoryId}/used")
    public boolean isSubCategoryUsed(@PathVariable Long subCategoryId) {
        return categoryService.isSubCategoryUsed(subCategoryId);
    }

    // 서브카테고리 수정
    @PutMapping("/subcategories/{subCategoryId}")
    public void updateSubCategory(
            @PathVariable Long subCategoryId,
            @RequestBody SubCategoryUpdateDto dto
    ) {
        categoryService.updateSubCategory(subCategoryId, dto);
    }
}
