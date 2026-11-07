package com.blinkweb.library.repository;

import com.blinkweb.library.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Override
    @Query("SELECT c FROM Category c WHERE c.id = :id")
    Optional<Category> findById(@Param("id") Long id);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name)")
    Optional<Category> findByNameIgnoreCase(@Param("name") String name);

    boolean existsByNameIgnoreCase(String name);
}
