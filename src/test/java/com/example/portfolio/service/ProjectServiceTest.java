package com.example.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.mapper.ProjectMapper;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;
import com.example.portfolio.repository.CategoryRepository;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;
import com.example.portfolio.repository.SubCategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectServiceTest {

    @Test
    void createsProjectWithResolvedCategoriesAndUploadedThumbnail() {
        ProjectRepository projectRepository = org.mockito.Mockito.mock(ProjectRepository.class);
        GcsService gcsService = org.mockito.Mockito.mock(GcsService.class);
        PhotoService photoService = org.mockito.Mockito.mock(PhotoService.class);
        PhotoRepository photoRepository = org.mockito.Mockito.mock(PhotoRepository.class);
        CategoryRepository categoryRepository = org.mockito.Mockito.mock(CategoryRepository.class);
        SubCategoryRepository subCategoryRepository = org.mockito.Mockito.mock(SubCategoryRepository.class);
        ProjectMapper mapper = new ProjectMapper() {};
        ProjectService service = new ProjectService(
                projectRepository,
                gcsService,
                photoService,
                mapper,
                photoRepository,
                categoryRepository,
                subCategoryRepository
        );
        Category category = new Category("Category");
        SubCategory subCategory = new SubCategory("Subcategory");
        category.addSubCategory(subCategory);
        MockMultipartFile thumbnail = new MockMultipartFile("thumbnail", "thumb.jpg", "image/jpeg", "image".getBytes());
        ProjectCreateDto dto = new ProjectCreateDto("Project", 1L, 2L, thumbnail, null);
        when(categoryRepository.getReferenceById(1L)).thenReturn(category);
        when(subCategoryRepository.getReferenceById(2L)).thenReturn(subCategory);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            ReflectionTestUtils.setField(project, "id", 10L);
            return project;
        });
        when(gcsService.uploadWebpFile(thumbnail, 10L)).thenReturn("https://example.com/thumb.webp");

        service.createProject(dto);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Project");
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getSubCategory()).isSameAs(subCategory);
        assertThat(saved.getThumbnailUrl()).isEqualTo("https://example.com/thumb.webp");
        verify(gcsService).deleteOnRollback("https://example.com/thumb.webp");
        verify(photoService).createPhotos(dto, 10L);
    }
}
