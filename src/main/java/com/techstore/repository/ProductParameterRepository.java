package com.techstore.repository;

import com.techstore.entity.ProductParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductParameterRepository extends JpaRepository<ProductParameter, Long> {
    @Query("SELECT DISTINCT pp.parameter.id, pp.parameter.nameEn, pp.parameter.nameBg, " +
            "pp.parameterOption.id, pp.parameterOption.nameEn, pp.parameterOption.nameBg, " +
            "pp.parameterOption.order " +
            "FROM ProductParameter pp " +
            "WHERE pp.product.category.id = :categoryId " +
            "AND pp.product.active = true " +
            "AND pp.product.status != com.techstore.enums.ProductStatus.NOT_AVAILABLE " +
            "AND pp.parameter IS NOT NULL " +
            "AND pp.parameterOption IS NOT NULL")
    List<Object[]> findParameterOptionsByCategoryAndActiveProducts(@Param("categoryId") Long categoryId);
}
