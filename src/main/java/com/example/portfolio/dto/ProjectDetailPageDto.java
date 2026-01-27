package com.example.portfolio.dto;

import org.springframework.data.domain.Slice;

public record ProjectDetailPageDto(
    String title,
    String thumbnailUrl,
    Slice<PhotoListDto> photos
) {}
