package com.example.portfolio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.Photo;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @InjectMocks
    private PhotoService photoService;

    @Mock
    private GcsService gcsService;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Test
    void uploadsPhotosAndAssignsTheirProject() {
        Category category = new Category("Category");
        SubCategory subCategory = new SubCategory("Subcategory");
        category.addSubCategory(subCategory);
        Project project = new Project("Project", category, subCategory);
        MultipartFile first = new MockMultipartFile("photos", "1.jpg", "image/jpeg", "1".getBytes());
        MultipartFile second = new MockMultipartFile("photos", "2.jpg", "image/jpeg", "2".getBytes());
        ProjectCreateDto dto = new ProjectCreateDto("Project", 1L, 2L, null, new MultipartFile[]{first, second});
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(gcsService.uploadWebpFile(any(MultipartFile.class), eq(10L)))
                .thenReturn("https://example.com/1.webp", "https://example.com/2.webp");

        photoService.createPhotos(dto, 10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Photo>> captor = ArgumentCaptor.forClass(List.class);
        verify(photoRepository).saveAll(captor.capture());
        verify(gcsService).deleteOnRollback("https://example.com/1.webp");
        verify(gcsService).deleteOnRollback("https://example.com/2.webp");
        assertThat(captor.getValue()).hasSize(2).allMatch(photo -> photo.getProject() == project);
    }
}
