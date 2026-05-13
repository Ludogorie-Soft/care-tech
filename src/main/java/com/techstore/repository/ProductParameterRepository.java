package com.techstore.repository;

import com.techstore.entity.ProductParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductParameterRepository extends JpaRepository<ProductParameter, Long> {
    @Query(value =
            "SELECT DISTINCT param.id, param.name_en, param.name_bg, " +
            "cp.is_filter, COALESCE(param.filter_order, param.sort_order), " +
            "po.id, po.name_en, po.name_bg, po.sort_order " +
            "FROM product_parameters pp " +
            "JOIN parameters          param ON param.id = pp.parameter_id " +
            "JOIN parameter_options   po    ON po.id    = pp.parameter_option_id " +
            "JOIN category_parameters cp    ON cp.parameter_id = param.id " +
            "                              AND cp.category_id  = :categoryId " +
            "JOIN products            p     ON p.id = pp.product_id " +
            "WHERE p.category_id = :categoryId " +
            "  AND p.active      = true " +
            "  AND p.show_flag   = true " +
            "  AND p.status IN ('AVAILABLE','ON_ROUTE','LIMITED_QUANTITY','ON_DEMAND') " +
            "  AND cp.is_filter  != false " +
            "ORDER BY COALESCE(param.filter_order, param.sort_order) ASC",
            nativeQuery = true)
    List<Object[]> findParameterOptionsByCategoryAndActiveProducts(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("DELETE FROM ProductParameter pp WHERE pp.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}
