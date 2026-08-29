package com.example.portfolio.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.portfolio.model.Category;
import com.example.portfolio.model.Photo;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsProjectRelationshipsThroughDomainMethods() {
        Category category = new Category("Category");
        SubCategory subCategory = new SubCategory("Subcategory");
        category.addSubCategory(subCategory);
        categoryRepository.saveAndFlush(category);

        Project project = new Project("Project", category, subCategory);
        project.changeThumbnailUrl("https://example.com/thumbnail.webp");
        project.addPhoto(new Photo("https://example.com/1.webp", "1.jpg", "image/webp"));
        project.addPhoto(new Photo("https://example.com/2.webp", "2.jpg", "image/webp"));
        Long projectId = projectRepository.saveAndFlush(project).getId();
        entityManager.clear();

        Project saved = projectRepository.findById(projectId).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Project");
        assertThat(saved.getCategory().getName()).isEqualTo("Category");
        assertThat(saved.getSubCategory().getName()).isEqualTo("Subcategory");
        assertThat(saved.getPhotos()).extracting(Photo::getImgoname)
                .containsExactlyInAnyOrder("1.jpg", "2.jpg");
    }
}
