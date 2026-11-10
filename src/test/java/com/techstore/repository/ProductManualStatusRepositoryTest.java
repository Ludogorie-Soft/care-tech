package com.techstore.repository;

import com.techstore.entity.Product;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The "missing from the feed" passes mark products NOT_AVAILABLE in bulk. A status an admin set by
 * hand (Product#manualStatus) outlives the product leaving the feed.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("ProductRepository — продукт с ръчен статус не се маркира неналичен, когато липсва във feed-а")
class ProductManualStatusRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ProductRepository repository;

    @Test
    @DisplayName("MOST/TEKRA: markNotAvailableByPlatformSkuNotIn и броенето преди него прескачат ръчния статус")
    void skuPassSkipsManualStatus() {
        Product inFeed = product(Platform.TEKRA, "IN-FEED", null, false);
        Product gone = product(Platform.TEKRA, "GONE", null, false);
        Product goneManual = product(Platform.TEKRA, "GONE-MANUAL", null, true);
        em.flush();

        List<String> feed = List.of("IN-FEED");
        assertThat(repository.countAvailableByPlatformSkuNotIn(Platform.TEKRA, feed)).isEqualTo(1);
        assertThat(repository.markNotAvailableByPlatformSkuNotIn(Platform.TEKRA, feed)).isEqualTo(1);
        em.clear();

        assertThat(statusOf(inFeed)).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(statusOf(gone)).isEqualTo(ProductStatus.NOT_AVAILABLE);
        assertThat(statusOf(goneManual)).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(em.find(Product.class, goneManual.getId()).getShow()).isTrue();
    }

    @Test
    @DisplayName("ASBIS: hideAsbisProductsNotIn прескача ръчния статус")
    void asbisPassSkipsManualStatus() {
        Product inFeed = product(Platform.ASBIS, "A-1", "A-1", false);
        Product gone = product(Platform.ASBIS, "A-2", "A-2", false);
        Product goneManual = product(Platform.ASBIS, "A-3", "A-3", true);
        em.flush();

        assertThat(repository.hideAsbisProductsNotIn(List.of("A-1"))).isEqualTo(1);
        em.clear();

        assertThat(statusOf(inFeed)).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(statusOf(gone)).isEqualTo(ProductStatus.NOT_AVAILABLE);
        assertThat(statusOf(goneManual)).isEqualTo(ProductStatus.AVAILABLE);
    }

    private ProductStatus statusOf(Product p) {
        return em.find(Product.class, p.getId()).getStatus();
    }

    private Product product(Platform platform, String sku, String asbisCode, boolean manualStatus) {
        Product p = new Product();
        p.setNameBg(sku);
        p.setSku(sku);
        p.setAsbisCode(asbisCode);
        p.setPlatform(platform);
        p.setFinalPrice(new BigDecimal("10.00"));
        p.setStatus(ProductStatus.AVAILABLE);
        p.setShow(true);
        p.setActive(true);
        p.setManualStatus(manualStatus);
        return em.persist(p);
    }
}
