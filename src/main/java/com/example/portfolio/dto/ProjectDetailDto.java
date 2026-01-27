package com.example.portfolio.dto;

import java.util.List;

public record ProjectDetailDto(
    Long id,
    String title,
    String imageUrl,     // 썸네일 URL
    Long categoryId,
    Long subCategoryId,
    List<PhotoListDto> photos
) {}
