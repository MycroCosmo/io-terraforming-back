package com.example.portfolio.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.dto.PhotoListDto;
import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.dto.ProjectDetailDto;
import com.example.portfolio.dto.ProjectDetailPageDto;
import com.example.portfolio.dto.ProjectListCustomDto;
import com.example.portfolio.dto.ProjectListDto;
import com.example.portfolio.dto.ProjectUpdateDto;
import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.mapper.ProjectMapper;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;
import com.example.portfolio.repository.CategoryRepository;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.repository.SubCategoryRepository;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final GcsService gcsService;
    private final PhotoService photoService;
    private final ProjectMapper projectMapper;
    private final PhotoRepository photoRepository;
    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;

    public ProjectService(ProjectRepository projectRepository,
                          GcsService gcsService,
                          PhotoService photoService,
                          ProjectMapper projectMapper,
                          PhotoRepository photoRepository,
                          CategoryRepository categoryRepository,
                          SubCategoryRepository subCategoryRepository) {
        this.projectRepository = projectRepository;
        this.gcsService = gcsService;
        this.photoService = photoService;
        this.projectMapper = projectMapper;
        this.photoRepository = photoRepository;
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "projectList", allEntries = true),
            @CacheEvict(value = "adminProjectList", allEntries = true)
    })
    public void createProject(ProjectCreateDto dto) {
        Category category = categoryRepository.getReferenceById(dto.categoryId());
        SubCategory subCategory = subCategoryRepository.getReferenceById(dto.subcategoryId());
        Project project = projectMapper.createDtoToProject(dto, category, subCategory);
        Long projectId = projectRepository.save(project).getId();

        MultipartFile thumbnail = dto.thumbnailMultipartFile();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            String url = gcsService.uploadWebpFile(thumbnail, projectId);
            gcsService.deleteOnRollback(url);
            project.changeThumbnailUrl(url);
        }

        photoService.createPhotos(dto, projectId);
        projectRepository.save(project);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "project", allEntries = true),
            @CacheEvict(value = "projectList", allEntries = true),
            @CacheEvict(value = "adminProjectList", allEntries = true),
            @CacheEvict(value = "adminProject", key = "#projectId")
    })
    public void updateProject(Long projectId, ProjectUpdateDto dto) {

        Project existing = projectRepository.findById(projectId)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FIND_PROJECT,
                        "Project not found with id: " + projectId
                ));

        // 업데이트는 "기존 엔티티 수정" 방식이 안전 (새 엔티티 만들어 save하면 관계/createdAt 꼬임)
        existing.changeTitle(dto.title());
        Category category = categoryRepository.getReferenceById(dto.categoryId());
        SubCategory sub = subCategoryRepository.getReferenceById(dto.subcategoryId());
        existing.changeCategory(category);
        existing.changeSubCategory(sub);

        // 썸네일 변경
        MultipartFile newThumb = dto.thumbnailMultipartFile();
        if (newThumb != null && !newThumb.isEmpty()) {
            String oldUrl = existing.getThumbnailUrl();
            String url = gcsService.uploadWebpFile(newThumb, projectId);
            gcsService.deleteOnRollback(url);
            gcsService.deleteAfterCommit(oldUrl);
            existing.changeThumbnailUrl(url);
        }

        // 사진 삭제
        if (dto.deletedPhotoIds() != null && !dto.deletedPhotoIds().isEmpty()) {
            photoService.deleteSelectedPhotos(dto.deletedPhotoIds());
        }

        // 사진 추가
        if (dto.photoMultipartFiles() != null && dto.photoMultipartFiles().length > 0) {
            photoService.addPhotos(projectId, dto);
        }

        projectRepository.save(existing);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "project", allEntries = true),
            @CacheEvict(value = "projectList", allEntries = true),
            @CacheEvict(value = "adminProjectList", allEntries = true),
            @CacheEvict(value = "adminProject", key = "#id")
    })
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FIND_PROJECT,
                "Project not found with id: " + id
        ));

        gcsService.deleteAfterCommit(project.getThumbnailUrl());
        photoService.deletePhotosByProjectId(id);

        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projectList",
            key = "(#categoryId != null ? #categoryId : 'all') + '-' + (#subCategoryId != null ? #subCategoryId : 'all') + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public Slice<ProjectListDto> getProjectList(Pageable pageable, Long categoryId, Long subCategoryId) {
        if (categoryId == null && subCategoryId == null) {
            return projectRepository.findAllProject(pageable);
        } else if (subCategoryId == null) {
            return projectRepository.findByCategory_id(pageable, categoryId);
        } else {
            return projectRepository.findBySubCategory_id(pageable, subCategoryId);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "adminProjectList", key = "#keyWord + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public ProjectListCustomDto getAdminProjectList(Pageable pageable, String keyWord) {
        Page<ProjectListDto> page = projectRepository.findByKeyWord(pageable, keyWord);
        return new ProjectListCustomDto(page.getContent(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "adminProject", key = "#projectId")
    public ProjectDetailDto getAdminProject(Long projectId) {
        ProjectDetailDto base = projectRepository.findProjectDetailByProjectId(projectId);
        List<PhotoListDto> photoList = photoRepository.findDetailPhotoByProjectId(projectId);

        // record 재생성 (ProjectDetailDto가 record라고 가정)
        return new ProjectDetailDto(
                base.id(),
                base.title(),
                base.imageUrl(),
                base.categoryId(),
                base.subCategoryId(),
                photoList
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "project", key = "#projectId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort")
    public ProjectDetailPageDto getPhotoList(Pageable pageable, Long projectId) {

        Project project = projectRepository.findById(projectId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FIND_PROJECT,
                "Project not found with id: " + projectId
        ));

        Slice<PhotoListDto> photos = photoRepository.findByPhotosProjectId(projectId, pageable);
        return new ProjectDetailPageDto(project.getTitle(), project.getThumbnailUrl(), photos);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "projectList", allEntries = true),
            @CacheEvict(value = "adminProjectList", allEntries = true)
    })
    public void updateViewCount(Long projectId) {
        projectRepository.updateViewCount(projectId);
    }
}
