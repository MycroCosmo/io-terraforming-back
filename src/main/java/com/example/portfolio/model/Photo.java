package com.example.portfolio.model;

import java.util.Objects;

import jakarta.persistence.*;

@Entity
@Table(name = "photo")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    private String imgoname;
    private String imgtype;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    protected Photo() {}

    public Photo(String imageUrl, String imgoname, String imgtype) {
        this.imageUrl = Objects.requireNonNull(imageUrl);
        this.imgoname = imgoname;
        this.imgtype = imgtype;
    }

    public Long getId() { return id; }
    public String getImageUrl() { return imageUrl; }
    public String getImgoname() { return imgoname; }
    public String getImgtype() { return imgtype; }
    public Project getProject() { return project; }

    // Project convenience에서 호출됨
    public void assignProject(Project project) {
        this.project = project;
    }
}
