package com.example.portfolio.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.portfolio.dto.PhotoListDto;
import com.example.portfolio.model.Photo;

public interface PhotoRepository extends JpaRepository<Photo, Long> {

    List<Photo> findByProject_Id(Long projectId);
    List<Photo> findAllByProject_Id(Long projectId);

    @Query("""
        SELECT new com.example.portfolio.dto.PhotoListDto(ph.id, ph.imageUrl)
        FROM Photo ph
        WHERE ph.project.id = :projectId
    """)
    Slice<PhotoListDto> findByPhotosProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("""
        SELECT new com.example.portfolio.dto.PhotoListDto(ph.id, ph.imageUrl)
        FROM Photo ph
        WHERE ph.project.id = :projectId
    """)
    List<PhotoListDto> findDetailPhotoByProjectId(@Param("projectId") Long projectId);
}
