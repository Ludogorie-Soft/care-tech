package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({PromotionExpiryService.class, ConcurrentMapCacheManager.class})
@DisplayName("PromotionExpiryService — срещу база: пипа само изтеклите промоции")
class PromotionExpiryServiceDbTest {

    @Autowired TestEntityManager entityManager;
    @Autowired PromotionExpiryService service;

    private final LocalDateTime now = LocalDateTime.now(PromotionExpiryService.PROMO_ZONE);

    private Product expired;
    private Product endsLater;
    private Product withoutEnd;
    private Category expiredCategory;
    private Category categoryEndsLater;
    private Manufacturer expiredBrand;
    private Manufacturer brandWithoutEnd;

    private Product productOnPromo(String name, LocalDateTime endsAt) {
        Product p = new Product();
        p.setNameBg(name);
        p.setPriceClient(new BigDecimal("100.00"));
        p.setDiscount(new BigDecimal("20"));
        p.setPromoEndsAt(endsAt);
        p.calculateFinalPrice();
        return entityManager.persist(p);
    }

    private Category categoryOnPromo(String name, LocalDateTime endsAt) {
        Category c = new Category();
        c.setNameBg(name);
        c.setIsPromoActive(true);
        c.setPromoDiscount(new BigDecimal("20"));
        c.setPromoEndsAt(endsAt);
        return entityManager.persist(c);
    }

    private Manufacturer brandOnPromo(String name, LocalDateTime endsAt) {
        Manufacturer m = new Manufacturer();
        m.setName(name);
        m.setIsPromoActive(true);
        m.setPromoDiscount(new BigDecimal("20"));
        m.setPromoEndsAt(endsAt);
        return entityManager.persist(m);
    }

    @BeforeEach
    void setUp() {
        expired = productOnPromo("Изтекла", now.minusMinutes(1));
        endsLater = productOnPromo("Изтича утре", now.plusDays(1));
        withoutEnd = productOnPromo("Без срок", null);
        expiredCategory = categoryOnPromo("Изтекла категория", now.minusMinutes(1));
        categoryEndsLater = categoryOnPromo("Категория до утре", now.plusDays(1));
        expiredBrand = brandOnPromo("Изтекла марка", now.minusMinutes(1));
        brandWithoutEnd = brandOnPromo("Марка без срок", null);
        entityManager.flush();

        service.expireDuePromotions();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Изтеклият продукт се връща на пълна цена")
    void expiredProductIsRestored() {
        Product reloaded = entityManager.find(Product.class, expired.getId());
        assertThat(reloaded.getDiscount()).isEqualByComparingTo("0");
        assertThat(reloaded.getPromoEndsAt()).isNull();
        assertThat(reloaded.getFinalPrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Продукт с бъдещ срок или без срок остава в промоция")
    void productsNotDueKeepTheirPromotion() {
        Product later = entityManager.find(Product.class, endsLater.getId());
        assertThat(later.getDiscount()).isEqualByComparingTo("20");
        assertThat(later.getPromoEndsAt()).isNotNull();
        assertThat(later.getFinalPrice()).isEqualByComparingTo("80.00");

        Product noEnd = entityManager.find(Product.class, withoutEnd.getId());
        assertThat(noEnd.getDiscount()).isEqualByComparingTo("20");
        assertThat(noEnd.getFinalPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("Само изтеклите категории и марки губят промоцията")
    void onlyExpiredCategoriesAndBrandsLoseThePromotion() {
        assertThat(entityManager.find(Category.class, expiredCategory.getId()).getIsPromoActive()).isFalse();
        assertThat(entityManager.find(Category.class, categoryEndsLater.getId()).getIsPromoActive()).isTrue();
        assertThat(entityManager.find(Manufacturer.class, expiredBrand.getId()).getIsPromoActive()).isFalse();
        assertThat(entityManager.find(Manufacturer.class, brandWithoutEnd.getId()).getIsPromoActive()).isTrue();
    }
}
