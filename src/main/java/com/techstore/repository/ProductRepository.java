package com.techstore.repository;

import com.techstore.dto.pazaruvaj.PazaruvajAttributeProjection;
import com.techstore.dto.pazaruvaj.PazaruvajProductProjection;
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByManufacturerId(Long manufacturerId);

    Optional<Product> findByReferenceNumber(String referenceNumber); // Added this method

    List<Product> findByExternalIdIn(Collection<Long> externalIds);

    List<Product> findByReferenceNumberIn(Collection<String> referenceNumbers);

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.show = true AND p.category.id = :categoryId AND p.status = com.techstore.enums.ProductStatus.AVAILABLE AND p.finalPrice > 0")
    Page<Product> findActiveByCategoryExcludingNotAvailable(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.show = true AND p.manufacturer.id = :brandId AND p.status = com.techstore.enums.ProductStatus.AVAILABLE AND p.finalPrice > 0")
    Page<Product> findActiveByManufacturerExcludingNotAvailable(@Param("brandId") Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.show = true AND p.id != :productId AND p.status = com.techstore.enums.ProductStatus.AVAILABLE AND " +
            "(p.category.id = :categoryId OR p.manufacturer.id = :manufacturerId)")
    List<Product> findRelatedProducts(@Param("productId") Long productId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("manufacturerId") Long manufacturerId,
                                      Pageable pageable);

    List<Product> findAllByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.slug IS NULL OR p.slug = ''")
    List<Product> findProductsWithoutSlug();

    @Query("SELECT p.sku, COUNT(p) FROM Product p WHERE p.sku IS NOT NULL GROUP BY p.sku HAVING COUNT(p) > 1")
    List<Object[]> findDuplicateProductsBySku();

    @Query("SELECT p.externalId, COUNT(p) FROM Product p WHERE p.externalId IS NOT NULL GROUP BY p.externalId HAVING COUNT(p) > 1")
    List<Object[]> findDuplicateProductsByExternalId();

    @Query("SELECT p FROM Product p WHERE p.externalId = :externalId")
    List<Product> findProductsByExternalId(@Param("externalId") Long externalId);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    List<Product> findProductsBySkuCode(@Param("sku") String sku);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND p.platform = :platform")
    List<Product> findBySkuAndPlatform(@Param("sku") String sku, @Param("platform") com.techstore.enums.Platform platform);

    /**
     * The category a product already sits in. Returns the id rather than the entity so the
     * caller does not touch a lazy association — with open-in-view disabled that would
     * throw outside a transaction.
     */
    @Query("SELECT p.category.id FROM Product p " +
            "WHERE p.sku = :sku AND p.platform = :platform AND p.category IS NOT NULL")
    List<Long> findCategoryIdsBySkuAndPlatform(@Param("sku") String sku,
                                               @Param("platform") com.techstore.enums.Platform platform);

    @Query("SELECT DISTINCT p.manufacturer FROM Product p " +
            "WHERE p.category.id = :categoryId " +
            "AND p.manufacturer IS NOT NULL")
    List<Manufacturer> findManufacturersByCategoryId(@Param("categoryId") Long categoryId);

    Page<Product> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    Page<Product> findByPlatformIsNullAndDeletedFalse(Pageable pageable);

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

    Page<Product> findByMarkupPercentageGreaterThanAndDeletedFalse(BigDecimal markup, Pageable pageable);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.asbisCode IN :asbisCodes")
    List<Product> findByAsbisCodeIn(@Param("asbisCodes") Collection<String> asbisCodes);

    @Modifying
    @Query("UPDATE Product p SET p.show = false, p.status = com.techstore.enums.ProductStatus.NOT_AVAILABLE " +
           "WHERE p.platform = com.techstore.enums.Platform.ASBIS " +
           "AND p.asbisCode NOT IN :knownCodes")
    int hideAsbisProductsNotIn(@Param("knownCodes") Collection<String> knownCodes);

    @Query("SELECT p.id AS id, p.slug AS slug, p.updatedAt AS updatedAt FROM Product p WHERE p.active = true AND p.show = true AND p.status = com.techstore.enums.ProductStatus.AVAILABLE AND p.slug IS NOT NULL AND p.slug <> ''")
    List<SitemapEntry> findSitemapEntries();

    @Query(value = """
        WITH RECURSIVE cat_tree AS (
            SELECT id FROM categories WHERE id = :categoryId
            UNION ALL
            SELECT c.id FROM categories c
            INNER JOIN cat_tree ct ON c.parent_id = ct.id
        )
        SELECT
            p.id                                                  AS id,
            COALESCE(p.name_bg, p.name_en)                       AS productName,
            p.slug                                                AS slug,
            p.final_price                                         AS finalPrice,
            p.image_url                                           AS primaryImageUrl,
            p.barcode                                             AS barcode,
            p.description_bg                                      AS descriptionBg,
            COALESCE(p.sku, '')                                   AS sku,
            COALESCE(m.name, '')                                  AS manufacturerName,
            COALESCE(c.name_bg, c.name_en, '')                   AS categoryName,
            COALESCE(cp.name_bg, cp.name_en, '')                 AS parentCategoryName
        FROM products p
        LEFT JOIN manufacturers m  ON m.id  = p.manufacturer_id
        LEFT JOIN categories    c  ON c.id  = p.category_id
        LEFT JOIN categories    cp ON cp.id = c.parent_id
        WHERE p.active     = true
          AND p.show_flag  = true
          AND p.status     = 'AVAILABLE'
          AND p.final_price > 0
          AND p.slug IS NOT NULL AND p.slug <> ''
          AND p.image_url IS NOT NULL AND p.image_url <> ''
          AND p.category_id IN (SELECT id FROM cat_tree)
        """, nativeQuery = true)
    List<PazaruvajProductProjection> findForPazaruvajFeedByCategory(@Param("categoryId") Long categoryId);

    @Query(value = """
        SELECT
            p.id                                                  AS id,
            COALESCE(p.name_bg, p.name_en)                       AS productName,
            p.slug                                                AS slug,
            p.final_price                                         AS finalPrice,
            p.image_url                                           AS primaryImageUrl,
            p.barcode                                             AS barcode,
            p.description_bg                                      AS descriptionBg,
            COALESCE(p.sku, '')                                   AS sku,
            COALESCE(m.name, '')                                  AS manufacturerName,
            COALESCE(c.name_bg, c.name_en, '')                   AS categoryName,
            COALESCE(cp.name_bg, cp.name_en, '')                 AS parentCategoryName
        FROM products p
        LEFT JOIN manufacturers m  ON m.id  = p.manufacturer_id
        LEFT JOIN categories    c  ON c.id  = p.category_id
        LEFT JOIN categories    cp ON cp.id = c.parent_id
        WHERE p.id IN (:productIds)
          AND p.active    = true
          AND p.show_flag = true
          AND p.status    = 'AVAILABLE'
          AND p.final_price > 0
          AND p.slug IS NOT NULL AND p.slug <> ''
        """, nativeQuery = true)
    List<PazaruvajProductProjection> findForPazaruvajFeedByProductIds(@Param("productIds") List<Long> productIds);

    @Query(value = """
        SELECT
            p.id                                                  AS id,
            COALESCE(p.name_bg, p.name_en)                       AS productName,
            p.slug                                                AS slug,
            p.final_price                                         AS finalPrice,
            p.image_url                                           AS primaryImageUrl,
            p.barcode                                             AS barcode,
            p.description_bg                                      AS descriptionBg,
            COALESCE(p.sku, '')                                   AS sku,
            COALESCE(m.name, '')                                  AS manufacturerName,
            COALESCE(c.name_bg, c.name_en, '')                   AS categoryName,
            COALESCE(cp.name_bg, cp.name_en, '')                 AS parentCategoryName
        FROM products p
        LEFT JOIN manufacturers m  ON m.id  = p.manufacturer_id
        LEFT JOIN categories    c  ON c.id  = p.category_id
        LEFT JOIN categories    cp ON cp.id = c.parent_id
        WHERE p.active     = true
          AND p.show_flag  = true
          AND p.status     = 'AVAILABLE'
          AND p.final_price > 0
          AND p.slug IS NOT NULL AND p.slug <> ''
          AND p.image_url IS NOT NULL AND p.image_url <> ''
        """, nativeQuery = true)
    List<PazaruvajProductProjection> findAllForPazaruvajFeed();

    @Query(value = """
        SELECT
            pp.product_id                                         AS productId,
            COALESCE(par.name_bg, par.name_en, '')               AS paramName,
            COALESCE(po.name_bg, po.name_en, '')                 AS paramValue
        FROM product_parameters pp
        JOIN parameters        par ON par.id = pp.parameter_id
        JOIN parameter_options po  ON po.id  = pp.parameter_option_id
        WHERE pp.product_id IN (:productIds)
          AND COALESCE(par.name_bg, par.name_en, '') <> ''
          AND COALESCE(po.name_bg,  po.name_en,  '') <> ''
        ORDER BY pp.product_id, par.name_bg
        """, nativeQuery = true)
    List<PazaruvajAttributeProjection> findAttributesForPazaruvajFeed(@Param("productIds") List<Long> productIds);

    @Modifying
    @Query("UPDATE Product p SET p.show = false WHERE p.sku IN :skus AND p.platform = :platform")
    int hideBySkuInAndPlatform(@Param("skus") Collection<String> skus, @Param("platform") Platform platform);

    @Modifying
    @Query("UPDATE Product p SET p.show = false, p.status = com.techstore.enums.ProductStatus.NOT_AVAILABLE " +
           "WHERE p.platform = :platform AND p.sku IS NOT NULL AND p.sku NOT IN :knownSkus")
    int markNotAvailableByPlatformSkuNotIn(@Param("platform") Platform platform, @Param("knownSkus") Collection<String> knownSkus);

    /** What {@link #markNotAvailableByPlatformSkuNotIn} would take off the shop: available products not in the feed. */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.platform = :platform AND p.sku IS NOT NULL " +
           "AND p.status <> com.techstore.enums.ProductStatus.NOT_AVAILABLE AND p.sku NOT IN :knownSkus")
    long countAvailableByPlatformSkuNotIn(@Param("platform") Platform platform, @Param("knownSkus") Collection<String> knownSkus);

    long countByPlatformAndStatusNot(Platform platform, ProductStatus status);

    List<Product> findByPlatformAndExternalIdNotNull(Platform platform);

    /**
     * Cross-platform deduplication by SKU.
     * <p>
     * For each SKU carried by 2+ platforms, shows the winner and hides the rest.
     * Winner = lowest final_price, tiebreaker = platform priority (VALI > TEKRA > ASBIS > MOST),
     * then lowest id. Called at the end of every sync so price changes are reflected.
     * <p>
     * The candidate set is every <em>sellable</em> row — active, AVAILABLE, priced, with an
     * image and not hidden by an admin — rather than every row that happens to be visible.
     * That distinction matters: keying off {@code show_flag = true} made this a one-way
     * ratchet. A row it had hidden was invisible to the next run, so it could never win
     * again even after becoming the cheapest, and once the winner went out of stock and was
     * hidden on its own merits, the whole SKU disappeared from the site. 91 SKUs were in
     * exactly that state (bug-460).
     * <p>
     * For the same reason this now assigns {@code show_flag} in both directions instead of
     * only hiding, and skips rows already in the right state so the returned count reflects
     * real changes.
     */
    @Modifying
    @Query(value = """
        WITH eligible AS (
            SELECT p.id, p.sku, p.platform, p.final_price, p.show_flag
            FROM products p
            WHERE p.sku IS NOT NULL
              AND p.active = true
              AND p.status = 'AVAILABLE'
              AND p.final_price > 0
              AND p.image_url IS NOT NULL AND p.image_url <> ''
              AND p.manually_hidden = false
        ),
        dup_skus AS (
            SELECT sku
            FROM eligible
            GROUP BY sku
            HAVING COUNT(DISTINCT platform) > 1
        ),
        ranked AS (
            SELECT
                e.id,
                e.show_flag,
                ROW_NUMBER() OVER (
                    PARTITION BY e.sku
                    ORDER BY
                        e.final_price ASC NULLS LAST,
                        CASE e.platform
                            WHEN 'VALI'  THEN 1
                            WHEN 'TEKRA' THEN 2
                            WHEN 'ASBIS' THEN 3
                            WHEN 'MOST'  THEN 4
                            ELSE 5
                        END,
                        e.id ASC
                ) AS rn
            FROM eligible e
            INNER JOIN dup_skus d ON d.sku = e.sku
        )
        UPDATE products p
        SET show_flag = (r.rn = 1), updated_at = NOW()
        FROM ranked r
        WHERE p.id = r.id
          AND p.show_flag IS DISTINCT FROM (r.rn = 1)
        """, nativeQuery = true)
    int deduplicateCrossPlatformBySku();
}