package com.example.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.portfolio.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Category -> SubCategory N+1 방지
    // DISTINCT 없으면 Category가 subCategories 수만큼 중복 row로 나옴
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.subCategories")
    List<Category> findAllWithSubCategories();
}
