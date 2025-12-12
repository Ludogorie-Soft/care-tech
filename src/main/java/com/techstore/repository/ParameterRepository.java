package com.techstore.repository;

import com.techstore.entity.Category;
import com.techstore.entity.Parameter;
import com.techstore.enums.Platform;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ParameterRepository extends JpaRepository<Parameter, Long> {

    List<Parameter> findByCategories_IdAndPlatform(Long categoryId, Platform platform);

    Page<Parameter> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Parameter p " +
            "JOIN FETCH p.categories c " +
            "WHERE c.id = :categoryId " +
            "AND EXISTS (" +
            "  SELECT 1 FROM Product prod " +
            "  WHERE prod.category.id = c.id " +
            "  AND prod.status != com.techstore.enums.ProductStatus.NOT_AVAILABLE " +
            "  AND prod.active = true" +
            ") " +
            "ORDER BY p.order ASC")
    List<Parameter> findParametersForAvailableProductsByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Parameter p JOIN FETCH p.categories c WHERE c.id = :categoryId")
    List<Parameter> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Parameter p JOIN FETCH p.categories c WHERE c.id = :categoryId ORDER BY p.order ASC")
    List<Parameter> findByCategoryIdOrderByOrderAsc(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Parameter p JOIN p.categories c WHERE c.id = :categoryId AND p.externalId IN :externalIds")
    List<Parameter> findByExternalIdInAndCategoryId(
            @Param("externalIds") Set<Long> externalIds,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT p FROM Parameter p JOIN p.categories c WHERE p.tekraKey = :tekraKey AND c.id = :categoryId")
    Optional<Parameter> findByTekraKeyAndCategoryId(
            @Param("tekraKey") String tekraKey,
            @Param("categoryId") Long categoryId
    );

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Parameter p JOIN p.categories c " +
            "WHERE LOWER(p.nameBg) = LOWER(:nameBg) AND c = :category")
    boolean existsByNameBgIgnoreCaseAndCategories(
            @Param("nameBg") String nameBg,
            @Param("category") Category category
    );

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Parameter p JOIN p.categories c " +
            "WHERE LOWER(p.nameEn) = LOWER(:nameEn) AND c = :category")
    boolean existsByNameEnIgnoreCaseAndCategories(
            @Param("nameEn") String nameEn,
            @Param("category") Category category
    );
}