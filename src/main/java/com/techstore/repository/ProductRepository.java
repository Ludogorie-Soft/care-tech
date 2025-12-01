package com.techstore.repository;

import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByManufacturerId(Long manufacturerId);

    Optional<Product> findByReferenceNumber(String referenceNumber); // Added this method

    List<Product> findByExternalIdIn(Collection<Long> externalIds);

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND p.status <> 'NOT_AVAILABLE'")
    Page<Product> findActiveByCategoryExcludingNotAvailable(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.manufacturer.id = :brandId AND p.status <> 'NOT_AVAILABLE'")
    Page<Product> findActiveByManufacturerExcludingNotAvailable(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.show = true AND p.id != :productId AND " +
            "(p.category.id = :categoryId OR p.manufacturer.id = :manufacturerId)")
    List<Product> findRelatedProducts(@Param("productId") Long productId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("manufacturerId") Long manufacturerId,
                                      Pageable pageable);

    List<Product> findAllByCategoryId(Long categoryId);

    @Query("SELECT p.sku, COUNT(p) FROM Product p WHERE p.sku IS NOT NULL GROUP BY p.sku HAVING COUNT(p) > 1")
    List<Object[]> findDuplicateProductsBySku();

    @Query("SELECT p.externalId, COUNT(p) FROM Product p WHERE p.externalId IS NOT NULL GROUP BY p.externalId HAVING COUNT(p) > 1")
    List<Object[]> findDuplicateProductsByExternalId();

    @Query("SELECT p FROM Product p WHERE p.externalId = :externalId")
    List<Product> findProductsByExternalId(@Param("externalId") Long externalId);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    List<Product> findProductsBySkuCode(@Param("sku") String sku);

    @Query("SELECT DISTINCT p.manufacturer FROM Product p " +
            "WHERE p.category.id = :categoryId " +
            "AND p.manufacturer IS NOT NULL")
    List<Manufacturer> findManufacturersByCategoryId(@Param("categoryId") Long categoryId);

    Page<Product> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    @Modifying
    @Query(value = """
    DELETE FROM product_parameters WHERE product_id = :productId;
    DELETE FROM product_flags WHERE product_id = :productId;
    DELETE FROM user_favorites WHERE product_id = :productId;
    DELETE FROM cart_items WHERE product_id = :productId;
    DELETE FROM additional_images WHERE product_id = :productId;
    DELETE FROM products WHERE id = :productId;
    """, nativeQuery = true)
    void permanentlyDeleteProductWithRelations(@Param("productId") Long productId);
}