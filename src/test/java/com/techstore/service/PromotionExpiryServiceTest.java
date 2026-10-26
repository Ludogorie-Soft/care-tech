package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionExpiryService — изтичане на промоции")
class PromotionExpiryServiceTest {

    @Mock ProductRepository      productRepository;
    @Mock CategoryRepository     categoryRepository;
    @Mock ManufacturerRepository manufacturerRepository;
    @Mock CacheManager           cacheManager;

    @InjectMocks PromotionExpiryService service;

    private static final LocalDateTime AN_HOUR_AGO = LocalDateTime.now(PromotionExpiryService.PROMO_ZONE).minusHours(1);

    @Test
    @DisplayName("Изтекъл продукт — отстъпка 0, без срок, цената е преизчислена")
    void expiredProductGetsFullPriceBack() {
        Product product = new Product();
        product.setPriceClient(new BigDecimal("100.00"));
        product.setMarkupPercentage(new BigDecimal("10"));
        product.setDiscount(new BigDecimal("20"));
        product.setPromoEndsAt(AN_HOUR_AGO);
        product.calculateFinalPrice();
        Cache cache = mock(Cache.class);
        when(productRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of(product));
        when(categoryRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());
        when(manufacturerRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());
        when(cacheManager.getCache("products")).thenReturn(cache);

        service.expireDuePromotions();

        assertEquals(BigDecimal.ZERO, product.getDiscount());
        assertNull(product.getPromoEndsAt());
        assertEquals(new BigDecimal("110.0000"), product.getFinalPrice().setScale(4));
        verify(productRepository).saveAll(List.of(product));
        verify(cache).clear();
    }

    @Test
    @DisplayName("Изтекли категория и марка — сваля се флагът, процентът и срокът")
    void expiredCategoryAndBrandLoseTheirFlag() {
        Category category = new Category();
        category.setIsPromoActive(true);
        category.setPromoDiscount(new BigDecimal("15"));
        category.setPromoEndsAt(AN_HOUR_AGO);
        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setIsPromoActive(true);
        manufacturer.setPromoDiscount(new BigDecimal("5"));
        manufacturer.setPromoEndsAt(AN_HOUR_AGO);
        when(productRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());
        when(categoryRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of(category));
        when(manufacturerRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of(manufacturer));

        service.expireDuePromotions();

        assertFalse(category.getIsPromoActive());
        assertNull(category.getPromoDiscount());
        assertNull(category.getPromoEndsAt());
        assertFalse(manufacturer.getIsPromoActive());
        assertNull(manufacturer.getPromoDiscount());
        assertNull(manufacturer.getPromoEndsAt());
    }

    @Test
    @DisplayName("Нищо не е изтекло — нищо не се записва и кешът остава")
    void nothingDueTouchesNothing() {
        when(productRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());
        when(categoryRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());
        when(manufacturerRepository.findByPromoEndsAtLessThanEqual(any())).thenReturn(List.of());

        service.expireDuePromotions();

        verify(productRepository, never()).saveAll(anyList());
        verify(cacheManager, never()).getCache(any());
    }
}
