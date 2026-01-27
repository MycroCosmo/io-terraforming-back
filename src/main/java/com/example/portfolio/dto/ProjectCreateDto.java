package com.example.portfolio.dto;

import org.springframework.web.multipart.MultipartFile;

public record ProjectCreateDto(
    String title,
    Long categoryId,
    Long subcategoryId,
    MultipartFile thumbnailMultipartFile,
    MultipartFile[] photoMultipartFiles
) {}
