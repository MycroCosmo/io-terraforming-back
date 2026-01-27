package com.example.portfolio.controller;

import java.io.IOException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.dto.ProjectDetailDto;
import com.example.portfolio.dto.ProjectDetailPageDto;
import com.example.portfolio.dto.ProjectListDto;
import com.example.portfolio.dto.ProjectUpdateDto;
import com.example.portfolio.service.ProjectService;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    // 생성
    @PostMapping
    public void create(@ModelAttribute ProjectCreateDto dto) {
        projectService.createProject(dto);
    }

    // 수정
    @PutMapping("/{projectId}")
    public void update(
        @PathVariable Long projectId,
        @ModelAttribute ProjectUpdateDto dto
    ) {
        projectService.updateProject(projectId, dto);
    }

    // 메인 프로젝트 목록
    @GetMapping
    public Slice<ProjectListDto> list(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long subCategoryId
    ) {
        return projectService.getProjectList(pageable, categoryId, subCategoryId);
    }

    // 관리자용 상세(기존 서비스 유지)
    @GetMapping("/{projectId}")
    public ProjectDetailPageDto detail(
        @PathVariable Long projectId,
        Pageable pageable
    ) {
        projectService.updateViewCount(projectId);
        return projectService.getPhotoList(pageable, projectId);
    }

    // 삭제
    @DeleteMapping("/{projectId}")
    public void delete(@PathVariable Long projectId) {
        projectService.deleteProject(projectId);
    }

    // 프로젝트 사진 페이지(조회수 증가 포함)
    @GetMapping("/{id}/photos")
    public ProjectDetailPageDto photos(
            @PageableDefault(size = 10) Pageable pageable,
            @PathVariable Long id
    ) {
        projectService.updateViewCount(id);
        return projectService.getPhotoList(pageable, id);
    }
}
