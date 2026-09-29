package com.example.portfolio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface ProjectMapper {

    default Project createDtoToProject(ProjectCreateDto dto, Category category, SubCategory subCategory) {
        return new Project(dto.title(), category, subCategory);
    }
}
