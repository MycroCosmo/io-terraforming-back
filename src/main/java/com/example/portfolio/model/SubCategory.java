package com.example.portfolio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "sub_category")
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @JsonIgnore
    private Category category;

    protected SubCategory() {
    }

    public SubCategory(String name) {
        this.name = name;
    }

    // getter
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    // 변경 메서드
    public void changeName(String name) {
        this.name = name;
    }

    // Category에서만 호출되도록 package-private 권장(같은 패키지면 가능)
    void setCategory(Category category) {
        this.category = category;
    }
}
