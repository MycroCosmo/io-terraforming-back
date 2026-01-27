package com.example.portfolio.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.*;

@Entity
@DynamicUpdate
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int view = 0;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    @Column(name = "created_at", updatable = false)
    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sub_category_id", nullable = false)
    private SubCategory subCategory;

    // Photo는 project 연관관계를 기준으로 관리 (projectId 필드 제거 전제)
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<Photo> photos = new ArrayList<>();

    protected Project() {
        // JPA
    }

    public Project(String title, Category category, SubCategory subCategory) {
        this.title = Objects.requireNonNull(title);
        this.category = Objects.requireNonNull(category);
        this.subCategory = Objects.requireNonNull(subCategory);
    }

    // ===== getters =====
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public int getView() { return view; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public Date getCreatedAt() { return createdAt; }
    public Category getCategory() { return category; }
    public SubCategory getSubCategory() { return subCategory; }
    public List<Photo> getPhotos() { return photos; }

    // ===== change methods (서비스에서 호출하는 것들) =====
    public void changeTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        this.title = title;
    }

    public void changeCategory(Category category) {
        this.category = Objects.requireNonNull(category);
    }

    public void changeSubCategory(SubCategory subCategory) {
        this.subCategory = Objects.requireNonNull(subCategory);
    }

    public void changeThumbnailUrl(String thumbnailUrl) {
        // null 허용 여부는 정책에 따라 결정
        this.thumbnailUrl = thumbnailUrl;
    }

    // ===== photo convenience =====
    public void addPhoto(Photo photo) {
        photos.add(Objects.requireNonNull(photo));
        photo.assignProject(this);
    }

    public void removePhoto(Photo photo) {
        photos.remove(photo);
        photo.assignProject(null);
    }

    @Override
    public String toString() {
        return "Project{id=" + id + ", title='" + title + "', view=" + view + "}";
    }
}
