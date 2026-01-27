package com.example.portfolio.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public record ProjectListDto(
    Long id,
    String title,
    String imageUrl, // 썸네일 URL

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    Date createdAt,

    int view,
    String categoryName,
    String subCategoryName,
    Long imageCount
) {}
