package com.techstore.repository;

import com.techstore.entity.ProductParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductParameterRepository extends JpaRepository<ProductParameter, Long> {
    @Modifying
    @Query("DELETE FROM ProductParameter pp WHERE pp.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}
