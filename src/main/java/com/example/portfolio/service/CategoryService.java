package com.example.portfolio.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.portfolio.dto.CategoryCreateDto;
import com.example.portfolio.dto.CategoryDto;
import com.example.portfolio.dto.CategoryUpdateDto;
import com.example.portfolio.dto.SubCategoryCreateDto;
import com.example.portfolio.dto.SubCategoryDto;
import com.example.portfolio.dto.SubCategoryUpdateDto;
import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.mapper.CategoryMapper;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.SubCategory;
import com.example.portfolio.repository.CategoryRepository;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.repository.SubCategoryRepository;

import jakarta.transaction.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProjectRepository projectRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           SubCategoryRepository subCategoryRepository,
                           ProjectRepository projectRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.projectRepository = projectRepository;
        this.categoryMapper = categoryMapper;
    }

    // (기존) 엔티티 raw 반환 - 가능하면 제거 권장
    public List<Category> getCategory() {
        return categoryRepository.findAll();
    }

    // 카테고리 전체 목록
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    /**
     * N+1 방지:
     * - Project를 기준으로 "사용 중인 category"를 가져오고
     * - category.subCategories를 fetch join으로 한 번에 로딩
     */
    @Cacheable(value = "category", key = "'categoryList'")
    public List<CategoryDto> getCategoriesWithProjects() {
        return projectRepository.findCategoriesWithProjectsFetchSubCategories().stream()
                .map(this::toCategoryDto)
                .toList();
        // mapper가 이미 record 대응이면 categoryMapper::categoryToDto로 바꿔도 됨
    }

    @Cacheable(value = "subCategory", key = "#categoryId")
    public List<SubCategoryDto> getSubCategoriesWithProjects(Long categoryId) {
        categoryRepository.findById(categoryId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category with ID: " + categoryId + " not found when retrieving subcategories"
        ));

        return projectRepository.findSubCategoriesWithProjects(categoryId).stream()
                .map(this::toSubCategoryDto)
                .toList();
    }

    /**
     * 생성(Create DTO -> Entity)
     * - Create DTO는 name만 있음
     * - 응답은 id가 필요하므로 CategoryDto로 반환 (DTO 이름 변경 없이, 기존 CategoryDto 사용)
     */
    @Transactional
    @CacheEvict(value = "category", key = "'categoryList'")
    public CategoryDto createCategories(CategoryCreateDto dto) {
        Category category = categoryMapper.createDtoToEntity(dto); // dto.name()만 사용하도록 mapper 수정 필요
        Category saved = categoryRepository.save(category);

        return toCategoryDto(saved);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", key = "'categoryList'"),
            @CacheEvict(value = "subCategory", allEntries = true)
    })
    public void deleteCategory(Long categoryId) {
        if (isCategoryUsed(categoryId)) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CATEGORY_IN_USE,
                    "Category ID: " + categoryId + " is referenced by existing projects"
            );
        }

        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category with ID: " + categoryId + " does not exist"
        ));

        categoryRepository.delete(category);
    }

    @Transactional
    public boolean isCategoryUsed(Long categoryId) {
        return projectRepository.existsByCategory_Id(categoryId);
    }

    /**
     * 서브카테고리 생성:
     * - categoryId는 PathVariable로 받음
     * - SubCategoryCreateDto는 name만 있음
     * - 응답은 id가 필요하므로 SubCategoryDto로 반환
     */
    @Transactional
    @CacheEvict(value = "subCategory", key = "#categoryId")
    public SubCategoryDto createSubCategory(Long categoryId, SubCategoryCreateDto dto) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category with ID: " + categoryId + " not found for creating subcategory"
        ));

        SubCategory subCategory = categoryMapper.createSubCategoryToSubCategory(dto, category);
        // mapper가 category까지 세팅하도록 강제(중요)

        SubCategory saved = subCategoryRepository.save(subCategory);
        return toSubCategoryDto(saved);
    }

    @Transactional
    @CacheEvict(value = "subCategory", allEntries = true)
    public void deleteSubCategory(Long subCategoryId) {
        if (isSubCategoryUsed(subCategoryId)) {
            throw new CustomException(
                    HttpStatus.CONFLICT,
                    ErrorCode.SUBCATEGORY_IN_USE,
                    "Subcategory ID: " + subCategoryId + " is referenced by existing projects"
            );
        }
        subCategoryRepository.deleteById(subCategoryId);
    }

    @Transactional
    public boolean isSubCategoryUsed(Long subCategoryId) {
        return projectRepository.existsBySubCategory_Id(subCategoryId);
    }

    /**
     * 업데이트:
     * - id는 PathVariable로 받음 (DTO에 id 필요 없음)
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "category", key = "'categoryList'"),
            @CacheEvict(value = "projectList", allEntries = true),
            @CacheEvict(value = "adminProjectList", allEntries = true)
    })
    public void updateCategory(Long categoryId, CategoryUpdateDto dto) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.CATEGORY_NOT_FOUND,
                "Category with ID: " + categoryId + " not found for update"
        ));

        category.changeName(dto.name()); // Category 엔티티에 changeName 필요
        // Dirty checking으로 반영되므로 save() 생략 가능
    }

    @Transactional
    @CacheEvict(value = "subCategory", allEntries = true)
    public void updateSubCategory(Long subCategoryId, SubCategoryUpdateDto dto) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.SUBCATEGORY_NOT_FOUND,
                "Subcategory with ID: " + subCategoryId + " not found for update"
        ));

        subCategory.changeName(dto.name()); // SubCategory 엔티티에 changeName 필요
    }

    // ===== Entity -> DTO =====
    private CategoryDto toCategoryDto(Category category) {
        List<SubCategoryDto> subs = (category.getSubCategories() == null)
                ? List.of()
                : category.getSubCategories().stream().map(this::toSubCategoryDto).toList();
        return new CategoryDto(category.getId(), category.getName(), subs);
    }

    private SubCategoryDto toSubCategoryDto(SubCategory subCategory) {
        return new SubCategoryDto(subCategory.getId(), subCategory.getName());
    }
}
