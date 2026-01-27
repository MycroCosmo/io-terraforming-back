package com.example.portfolio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.model.Project;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ProjectMapper {

    // 연관관계(category, subCategory)는 서비스에서 주입할 것
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "view", ignore = true)
    @Mapping(target = "photos", ignore = true)
    Project createDtoToProject(ProjectCreateDto dto);
}
