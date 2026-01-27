package com.example.portfolio.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.example.portfolio.dto.ProjectDetailDto;
import com.example.portfolio.dto.ProjectListCustomDto;
import com.example.portfolio.service.ProjectService;

@RestController
@RequestMapping("/api/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;

    public AdminProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ProjectListCustomDto list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "") String keyWord
    ) {
        Sort sortOrder = direction.equalsIgnoreCase("asc")
                ? Sort.by(sort).ascending()
                : Sort.by(sort).descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return projectService.getAdminProjectList(pageable, keyWord);
    }

    @GetMapping("/{projectId}")
    public ProjectDetailDto detail(@PathVariable Long projectId) {
        return projectService.getAdminProject(projectId);
    }
}
