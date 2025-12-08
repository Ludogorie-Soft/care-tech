package com.techstore.repository;

import com.techstore.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    Page<Category> findByShowTrue(Pageable pageable);

    Optional<Category> findByNameBg(String nameBg);

    List<Category> findByIsPromoActiveTrue();

    List<Category> findDistinctByProductsMarkupPercentageGreaterThan(BigDecimal markup);
}