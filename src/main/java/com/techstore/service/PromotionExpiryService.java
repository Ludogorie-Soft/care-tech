package com.techstore.service;

import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Ends promotions whose end date has passed.
 *
 * Products carry their own end date (a category or brand promotion copies it onto every
 * product), so prices are restored product by product; categories and brands only lose
 * their "on promotion" flag. A sync that saves a stale product writes the old, already
 * passed end date back as well, so the next run expires it again.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionExpiryService {

    public static final ZoneId PROMO_ZONE = ZoneId.of("Europe/Sofia");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final CacheManager cacheManager;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireDuePromotions() {
        LocalDateTime now = LocalDateTime.now(PROMO_ZONE);

        List<Product> products = productRepository.findByPromoEndsAtLessThanEqual(now);
        for (Product product : products) {
            product.setDiscount(BigDecimal.ZERO);
            product.setPromoEndsAt(null);
            product.calculateFinalPrice();
        }

        List<Category> categories = categoryRepository.findByPromoEndsAtLessThanEqual(now);
        for (Category category : categories) {
            category.setIsPromoActive(false);
            category.setPromoDiscount(null);
            category.setPromoEndsAt(null);
        }

        List<Manufacturer> manufacturers = manufacturerRepository.findByPromoEndsAtLessThanEqual(now);
        for (Manufacturer manufacturer : manufacturers) {
            manufacturer.setIsPromoActive(false);
            manufacturer.setPromoDiscount(null);
            manufacturer.setPromoEndsAt(null);
        }

        if (products.isEmpty() && categories.isEmpty() && manufacturers.isEmpty()) {
            return;
        }

        productRepository.saveAll(products);
        categoryRepository.saveAll(categories);
        manufacturerRepository.saveAll(manufacturers);

        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            cache.clear();
        }

        log.info("Expired promotions: {} products, {} categories, {} brands",
                products.size(), categories.size(), manufacturers.size());
    }
}
