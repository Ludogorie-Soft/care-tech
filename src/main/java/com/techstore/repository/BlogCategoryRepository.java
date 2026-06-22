package com.techstore.repository;

import com.techstore.entity.BlogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogCategoryRepository extends JpaRepository<BlogCategory, Long> {

    Optional<BlogCategory> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<BlogCategory> findAllByOrderBySortOrderAscNameAsc();

    List<BlogCategory> findByParentIsNullOrderBySortOrderAscNameAsc();
}
