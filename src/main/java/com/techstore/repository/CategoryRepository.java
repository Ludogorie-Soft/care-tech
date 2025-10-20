package com.techstore.repository;

import com.techstore.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByExternalId(Long externalId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findByShowTrueOrderBySortOrderAscNameEnAsc();

    Page<Category> findByShowTrue(Pageable pageable);

    Optional<Category> findByTekraSlug(String tekraSlug);

    Optional<Category> findByNameBg(String nameBg);

    Optional<Category> findByNameEn(String nameEn);

    List<Category> findByParentId(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.asbisId = :id")
    Optional<Category> findByAsbisId(@Param("id") String id);

    @Query("SELECT c FROM Category c WHERE c.asbisCode = :code")
    Optional<Category> findByAsbisCode(@Param("code") String code);

    @Query("SELECT c FROM Category c WHERE c.asbisId IS NOT NULL")
    List<Category> findAllAsbisCategories();

    @Query("SELECT COUNT(c) FROM Category c WHERE c.asbisId IS NOT NULL")
    Long countAsbisCategories();
}