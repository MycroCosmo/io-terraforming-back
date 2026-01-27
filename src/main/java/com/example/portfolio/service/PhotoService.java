package com.example.portfolio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.dto.ProjectUpdateDto;
import com.example.portfolio.exception.CustomException;
import com.example.portfolio.exception.ErrorCode;
import com.example.portfolio.model.Photo;
import com.example.portfolio.model.Project;
import com.example.portfolio.repository.PhotoRepository;
import com.example.portfolio.repository.ProjectRepository;

@Service
public class PhotoService {

    private final GcsService gcsService;
    private final PhotoRepository photoRepository;
    private final ProjectRepository projectRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public PhotoService(GcsService gcsService, PhotoRepository photoRepository, ProjectRepository projectRepository) {
        this.gcsService = gcsService;
        this.photoRepository = photoRepository;
        this.projectRepository = projectRepository;
    }

    public void createPhotos(ProjectCreateDto dto, Long projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FIND_PROJECT,
                "Project not found with id: " + projectId
        ));

        MultipartFile[] multipartFiles = dto.photoMultipartFiles();
        if (multipartFiles == null || multipartFiles.length == 0) return;

        List<CompletableFuture<Photo>> futures = new ArrayList<>();

        for (MultipartFile multipartFile : multipartFiles) {
            CompletableFuture<Photo> future = CompletableFuture.supplyAsync(() -> {
                String url = gcsService.uploadWebpFile(multipartFile, projectId);

                Photo photo = new Photo(
                        url,
                        multipartFile.getOriginalFilename(),
                        "image/webp"
                );

                // 관계 연결
                project.addPhoto(photo);
                return photo;
            }, executorService);

            futures.add(future);
        }

        List<Photo> photos = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
        photoRepository.saveAll(photos);
    }

    // 사진 추가 업로드(업데이트 시)
    public void addPhotos(Long projectId, ProjectUpdateDto dto) {
        Project project = projectRepository.findById(projectId).orElseThrow(() -> new CustomException(
                HttpStatus.NOT_FOUND,
                ErrorCode.NOT_FIND_PROJECT,
                "Project not found with id: " + projectId
        ));

        MultipartFile[] multipartFiles = dto.photoMultipartFiles();
        if (multipartFiles == null || multipartFiles.length == 0) return;

        for (MultipartFile multipartFile : multipartFiles) {
            String url = gcsService.uploadWebpFile(multipartFile, projectId);

            Photo photo = new Photo(
                    url,
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType()
            );

            project.addPhoto(photo);
            photoRepository.save(photo);
        }
    }

    // 프로젝트 삭제 시 전체 사진 삭제(메타 + GCS)
    public void deletePhotosByProjectId(Long projectId) {
        List<Photo> photos = photoRepository.findAllByProjectId(projectId);
        photoRepository.deleteAll(photos);
        gcsService.deletePhotoToGcs(photos);
    }

    // edit에서 선택 삭제
    public void deleteSelectedPhotos(List<Long> deletedPhotoIds) {
        if (deletedPhotoIds == null || deletedPhotoIds.isEmpty()) return;

        List<Photo> selected = photoRepository.findAllById(deletedPhotoIds);
        photoRepository.deleteAll(selected);
        gcsService.deletePhotoToGcs(selected);
    }
}
