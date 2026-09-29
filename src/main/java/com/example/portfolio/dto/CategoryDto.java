package com.example.portfolio.dto;

import java.util.List;

public record CategoryDto(
    Long id,
    String name,
    List<SubCategoryDto> subCategories
) {}
