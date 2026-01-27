package com.example.portfolio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.example.portfolio.dto.CategoryCreateDto;
import com.example.portfolio.dto.CategoryDto;
import com.example.portfolio.dto.CategoryUpdateDto;
import com.example.portfolio.dto.SubCategoryCreateDto;
import com.example.portfolio.dto.SubCategoryDto;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.SubCategory;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    // ===== Category =====

    // Entity -> Response DTO
    CategoryDto categoryToDto(Category category);

    // Create DTO -> Entity
    // (Create DTO는 name만 있다고 가정)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subCategories", ignore = true) // 생성 시점엔 보통 없음
    Category createDtoToEntity(CategoryCreateDto dto);

    // Update는 보통 "엔티티 찾아서 changeName()"로 처리하므로 mapper 불필요
    // 남겨야 한다면 아래처럼 "name만" 쓰는 방식으로 제한
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subCategories", ignore = true)
    Category updateDtoToEntity(CategoryUpdateDto dto);

    // ===== SubCategory =====

    // Entity -> Response DTO
    SubCategoryDto subCategoryToDto(SubCategory subCategory);

    // Create DTO -> Entity (category는 PathVariable로 받은 Category를 주입)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    SubCategory createSubCategoryToSubCategory(SubCategoryCreateDto dto, Category category);
}
