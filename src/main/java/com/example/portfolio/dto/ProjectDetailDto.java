package com.example.portfolio.dto;

import java.util.List;

public record ProjectDetailDto(
    Long id,
    String title,
    String imageUrl,     // 썸네일 URL
    Long categoryId,
    Long subCategoryId,
    List<PhotoListDto> photos
) {
    public ProjectDetailDto(Long id, String title, String imageUrl,
                            Long categoryId, Long subCategoryId) {
        this(id, title, imageUrl, categoryId, subCategoryId, List.of());
    }
}
