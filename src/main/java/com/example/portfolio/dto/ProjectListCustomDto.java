package com.example.portfolio.dto;

import java.util.List;

public record ProjectListCustomDto(
    List<ProjectListDto> content,
    int totalPages
) {}
