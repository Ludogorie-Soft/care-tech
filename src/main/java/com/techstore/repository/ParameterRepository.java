package com.techstore.repository;

import com.techstore.entity.Category;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface
ParameterRepository extends JpaRepository<Parameter, Long> {

    Optional<Parameter> findByExternalId(Long externalId);

    List<Parameter> findAllByCategoryId(Long categoryId);

    boolean existsByNameBgIgnoreCaseAndCategory(String nameBg, Category category);
    boolean existsByNameEnIgnoreCaseAndCategory(String nameEn, Category category);

    List<Parameter> findByCategoryIdOrderByOrderAsc(Long categoryId);

    @Query("SELECT DISTINCT p FROM Parameter p " +
            "JOIN p.category c " +
            "WHERE c.id = :categoryId " +
            "AND EXISTS (SELECT prod FROM Product prod WHERE prod.category.id = c.id AND prod.status != com.techstore.enums.ProductStatus.NOT_AVAILABLE AND prod.active = true) " +
            "ORDER BY p.order ASC")
    List<Parameter> findParametersForAvailableProductsByCategory(@Param("categoryId") Long categoryId);

    Optional<Parameter> findByExternalIdAndCategoryId(Long externalId, Long categoryId);

    List<Parameter> findByExternalIdInAndCategoryId(Collection<Long> externalIds, Long categoryId); // New method

    Optional<Parameter> findByCategoryAndNameBg(Category category, String nameBg);

    List<Parameter> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    Optional<Parameter> findByTekraKeyAndCategoryId(String tekraKey, Long categoryId);

    @Query("SELECT p FROM Parameter p WHERE p.asbisKey = :key AND p.category.id = :categoryId")
    Optional<Parameter> findByAsbisKeyAndCategoryId(@Param("key") String key, @Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Parameter p WHERE p.asbisKey IS NOT NULL")
    List<Parameter> findAllAsbisParameters();

    @Query("SELECT COUNT(p) FROM Parameter p WHERE p.asbisKey IS NOT NULL")
    Long countAsbisParameters();

    @Query(value = "SELECT DISTINCT ON (name_bg, name_en) * " +
            "FROM parameters " +
            "WHERE category_id = :categoryId " +
            "ORDER BY name_bg, name_en, sort_order ASC",
            nativeQuery = true)
    List<Parameter> findUniqueByCategoryId(@Param("categoryId") Long categoryId);


}