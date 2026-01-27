package com.example.portfolio.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.portfolio.dto.ProjectDetailDto;
import com.example.portfolio.dto.ProjectListDto;
import com.example.portfolio.model.Category;
import com.example.portfolio.model.Project;
import com.example.portfolio.model.SubCategory;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // ===== 메인 페이지 목록 =====
    @Query("""
        SELECT new com.example.portfolio.dto.ProjectListDto(
            p.id, p.title, p.thumbnailUrl, p.createdAt, p.view,
            p.category.name, p.subCategory.name, NULL
        )
        FROM Project p
    """)
    Slice<ProjectListDto> findAllProject(Pageable pageable);

    @Query("""
        SELECT new com.example.portfolio.dto.ProjectListDto(
            p.id, p.title, p.thumbnailUrl, p.createdAt, p.view,
            p.category.name, p.subCategory.name, NULL
        )
        FROM Project p
        WHERE p.category.id = :categoryId
    """)
    Slice<ProjectListDto> findByCategory_id(Pageable pageable, @Param("categoryId") Long categoryId);

    @Query("""
        SELECT new com.example.portfolio.dto.ProjectListDto(
            p.id, p.title, p.thumbnailUrl, p.createdAt, p.view,
            p.category.name, p.subCategory.name, NULL
        )
        FROM Project p
        WHERE p.subCategory.id = :subCategoryId
    """)
    Slice<ProjectListDto> findBySubCategory_id(Pageable pageable, @Param("subCategoryId") Long subCategoryId);

    // ===== 관리자 검색 + 이미지 수 카운트 =====
    // (중요) Photo.projectId 기반 -> 관계 기반으로 변경: LEFT JOIN p.photos
    @Query("""
        SELECT new com.example.portfolio.dto.ProjectListDto(
            p.id, p.title, p.thumbnailUrl, p.createdAt, p.view,
            p.category.name, p.subCategory.name, COUNT(ph)
        )
        FROM Project p
        LEFT JOIN p.photos ph
        WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyWord, '%'))
        GROUP BY p.id, p.title, p.thumbnailUrl, p.createdAt, p.view, p.category.name, p.subCategory.name
    """)
    Page<ProjectListDto> findByKeyWord(Pageable pageable, @Param("keyWord") String keyWord);

    // ===== 카테고리/서브카테고리 조회 =====
    @Query("""
        SELECT DISTINCT p.category
        FROM Project p
        WHERE p.category IS NOT NULL
    """)
    List<Category> findCategoriesWithProjects();

    @Query("""
        SELECT DISTINCT p.subCategory
        FROM Project p
        WHERE p.subCategory IS NOT NULL AND p.category.id = :categoryId
    """)
    List<SubCategory> findSubCategoriesWithProjects(@Param("categoryId") Long categoryId);

    // ✅ N+1 방지용: 카테고리 + subCategories fetch join
    // CategoryService.getCategoriesWithProjects()에서 이걸 쓰면 category.getSubCategories() 접근해도 추가쿼리 없음
    @Query("""
        SELECT DISTINCT c
        FROM Project p
        JOIN p.category c
        LEFT JOIN FETCH c.subCategories sc
    """)
    List<Category> findCategoriesWithProjectsFetchSubCategories();

    // ===== 사용 여부 체크 =====
    boolean existsByCategory_Id(Long categoryId);
    boolean existsBySubCategory_Id(Long subCategoryId);

    // ===== 조회수 업데이트 =====
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Project p SET p.view = p.view + 1 WHERE p.id = :projectId")
    void updateViewCount(@Param("projectId") Long projectId);

    // ===== 상세 DTO =====
    @Query("""
        SELECT new com.example.portfolio.dto.ProjectDetailDto(
            p.id, p.title, p.thumbnailUrl, c.id, s.id
        )
        FROM Project p
        JOIN p.category c
        JOIN p.subCategory s
        WHERE p.id = :projectId
    """)
    ProjectDetailDto findProjectDetailByProjectId(@Param("projectId") Long projectId);
}
