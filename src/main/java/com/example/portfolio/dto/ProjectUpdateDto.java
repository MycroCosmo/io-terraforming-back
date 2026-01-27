package com.example.portfolio.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public record ProjectUpdateDto(
    Long id,
    String title,
    Long categoryId,
    Long subcategoryId,
    MultipartFile thumbnailMultipartFile,
    MultipartFile[] photoMultipartFiles,
    List<Long> deletedPhotoIds
) {}
