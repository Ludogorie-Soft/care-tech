package com.techstore.config;


import com.techstore.entity.Product;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlugRegenerationRunner implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        List<Product> productsWithoutSlug = productRepository.findAll()
                .stream()
                .filter(p -> p.getSlug() == null || p.getSlug().isEmpty())
                .toList();

        if (productsWithoutSlug.isEmpty()) {
            log.info("All slugs already exist, skipping regeneration");
            return;
        }

        log.info("Starting slug regeneration for {} products...", productsWithoutSlug.size());

        try {
            for (Product product : productsWithoutSlug) {
                product.generateSlug();
            }

            productRepository.saveAll(productsWithoutSlug);
            log.info("Successfully regenerated {} slugs", productsWithoutSlug.size());
        } catch (Exception e) {
            log.error("Error during slug regeneration: {}", e.getMessage(), e);
        }
    }
}