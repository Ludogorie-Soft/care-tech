package com.techstore.repository;

import com.techstore.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByExternalIdIn(Set<Long> externalIds);

    Optional<Category> findByExternalId(Long externalId);

    Optional<Category> findBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<Category> findByShowTrue();

    Page<Category> findByShowTrue(Pageable pageable);

    Optional<Category> findByNameBg(String nameBg);

    List<Category> findByIsPromoActiveTrue();

    List<Category> findDistinctByProductsMarkupPercentageGreaterThan(BigDecimal markup);

    List<Category> findByParentId(Long parentId);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parent")
    List<Category> findAllWithParents();

    @Query("SELECT c.id AS id, c.slug AS slug, c.updatedAt AS updatedAt FROM Category c WHERE c.show = true AND c.slug IS NOT NULL AND c.slug <> ''")
    List<SitemapEntry> findSitemapEntries();
}