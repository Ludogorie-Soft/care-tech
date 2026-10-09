package com.techstore.service.admin;

import com.techstore.dto.request.ProductPromoRequest;
import com.techstore.entity.Category;
import com.techstore.entity.Product;
import com.techstore.exception.BusinessLogicException;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.PromotionExpiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService — промоции с краен срок")
class AdminServicePromoTest {

    @Mock ManufacturerRepository manufacturerRepository;
    @Mock ProductRepository      productRepository;
    @Mock CategoryRepository     categoryRepository;
    @Mock ParameterMapper        parameterMapper;
    @Mock CacheManager           cacheManager;

    @InjectMocks AdminService service;

    private static final LocalDateTime NEXT_WEEK = LocalDateTime.now(PromotionExpiryService.PROMO_ZONE).plusDays(7);

    private Product product(long id) {
        Product p = new Product();
        p.setId(id);
        p.setPriceClient(new BigDecimal("100.00"));
        return p;
    }

    private ProductPromoRequest request(long productId, String discount, LocalDateTime endsAt) {
        ProductPromoRequest r = new ProductPromoRequest();
        r.setId(productId);
        r.setDiscount(new BigDecimal(discount));
        r.setPromoEndsAt(endsAt);
        return r;
    }

    @Test
    @DisplayName("Промоция на продукт записва отстъпката, срока и новата цена")
    void productPromoStoresEndDate() {
        Product product = product(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.createPromo(request(1L, "10", NEXT_WEEK), "bg");

        assertEquals(new BigDecimal("10"), product.getDiscount());
        assertEquals(NEXT_WEEK, product.getPromoEndsAt());
        assertEquals(new BigDecimal("90.00"), product.getFinalPrice());
    }

    @Test
    @DisplayName("Премахната промоция нулира и срока")
    void removingPromoClearsEndDate() {
        Product product = product(1L);
        product.setDiscount(new BigDecimal("10"));
        product.setPromoEndsAt(NEXT_WEEK);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        service.createPromo(request(1L, "0", null), "bg");

        assertEquals(BigDecimal.ZERO, product.getDiscount());
        assertNull(product.getPromoEndsAt());
        assertEquals(new BigDecimal("100.00"), product.getFinalPrice());
    }

    @Test
    @DisplayName("Минал срок или отстъпка от 100% се отказват, без да се пипа продуктът")
    void invalidPromoIsRejected() {
        LocalDateTime yesterday = LocalDateTime.now(PromotionExpiryService.PROMO_ZONE).minusDays(1);

        assertThrows(BusinessLogicException.class, () -> service.createPromo(request(1L, "10", yesterday), "bg"));
        assertThrows(BusinessLogicException.class, () -> service.createPromo(request(1L, "100", null), "bg"));
        assertThrows(BusinessLogicException.class, () -> service.createPromo(request(1L, "-1", null), "bg"));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Промоция на категория пази процента и срока и ги копира на продуктите ѝ")
    void categoryPromoCopiesEndDateToProducts() {
        Category category = new Category();
        category.setId(5L);
        Product first = product(1L);
        Product second = product(2L);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryId(5L)).thenReturn(List.of(first, second));

        service.createPromoByCategory(5L, new BigDecimal("15"), NEXT_WEEK, "bg");

        assertTrue(category.getIsPromoActive());
        assertEquals(new BigDecimal("15"), category.getPromoDiscount());
        assertEquals(NEXT_WEEK, category.getPromoEndsAt());
        assertEquals(NEXT_WEEK, first.getPromoEndsAt());
        assertEquals(new BigDecimal("85.00"), second.getFinalPrice());
    }

    @Test
    @DisplayName("Премахната промоция на категория нулира флага, процента и срока")
    void removingCategoryPromoClearsEverything() {
        Category category = new Category();
        category.setId(5L);
        category.setIsPromoActive(true);
        category.setPromoDiscount(new BigDecimal("15"));
        category.setPromoEndsAt(NEXT_WEEK);
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(productRepository.findAllByCategoryId(5L)).thenReturn(List.of());

        service.createPromoByCategory(5L, BigDecimal.ZERO, NEXT_WEEK, "bg");

        assertFalse(category.getIsPromoActive());
        assertNull(category.getPromoDiscount());
        assertNull(category.getPromoEndsAt());
    }
}
