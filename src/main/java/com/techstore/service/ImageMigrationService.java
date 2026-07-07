package com.techstore.service;

import com.techstore.entity.Product;
import com.techstore.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageMigrationService {

    private final ProductRepository productRepository;
    private final S3Service s3Service;

    // AtomicBoolean ensures compareAndSet is atomic — no race condition between check and set
    private final AtomicBoolean migrationRunning = new AtomicBoolean(false);

    public boolean isMigrationRunning() {
        return migrationRunning.get();
    }

    @Async
    public void migrateAllValiImagesToS3() {
        // compareAndSet(false, true) atomically: if currently false, set to true and proceed
        if (!migrationRunning.compareAndSet(false, true)) {
            log.warn("Image migration already in progress, skipping");
            return;
        }

        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed   = new AtomicInteger(0);
        AtomicInteger skipped  = new AtomicInteger(0);

        log.info("=== Image migration to S3 started ===");
        try {
            int page     = 0;
            int pageSize = 50;
            List<Product> batch;

            do {
                batch = productRepository.findAll(PageRequest.of(page, pageSize)).getContent();
                for (Product product : batch) {
                    migrateProduct(product, migrated, failed, skipped);
                }
                page++;
                log.info("Migration progress — page={} migrated={} failed={} skipped={}",
                        page, migrated.get(), failed.get(), skipped.get());

                // Small pause between pages to avoid hammering vali.bg
                Thread.sleep(200);

            } while (batch.size() == pageSize);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Image migration interrupted");
        } catch (Exception e) {
            log.error("Image migration encountered an error: {}", e.getMessage(), e);
        } finally {
            migrationRunning.set(false);
            log.info("=== Image migration finished — migrated={} failed={} skipped={} ===",
                    migrated.get(), failed.get(), skipped.get());
        }
    }

    // @Transactional intentionally omitted:
    // This method is called via this.migrateProduct() (self-invocation), which bypasses
    // Spring's AOP proxy — the annotation would be a no-op. Each productRepository.save()
    // runs in its own transaction provided by Spring Data JPA.
    private void migrateProduct(Product product,
                                 AtomicInteger migrated,
                                 AtomicInteger failed,
                                 AtomicInteger skipped) {
        try {
            boolean changed = false;

            // Primary image
            if (isExternalUrl(product.getPrimaryImageUrl())) {
                String s3Url = s3Service.uploadImageFromUrl(product.getPrimaryImageUrl(), "products/vali");
                if (s3Url != null) {
                    product.setPrimaryImageUrl(s3Url);
                    changed = true;
                    migrated.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            } else {
                skipped.incrementAndGet();
            }

            // Additional images
            if (product.getAdditionalImages() != null && !product.getAdditionalImages().isEmpty()) {
                List<String> updated = new ArrayList<>();
                boolean anyChanged = false;
                for (String url : product.getAdditionalImages()) {
                    if (isExternalUrl(url)) {
                        String s3Url = s3Service.uploadImageFromUrl(url, "products/vali");
                        if (s3Url != null) {
                            updated.add(s3Url);
                            migrated.incrementAndGet();
                            anyChanged = true;
                        } else {
                            updated.add(url);
                            failed.incrementAndGet();
                        }
                    } else {
                        updated.add(url);
                        skipped.incrementAndGet();
                    }
                }
                if (anyChanged) {
                    product.setAdditionalImages(updated);
                    changed = true;
                }
            }

            if (changed) {
                productRepository.save(product);
            }
        } catch (Exception e) {
            log.error("Failed to migrate images for product {}: {}", product.getId(), e.getMessage());
            failed.incrementAndGet();
        }
    }

    // Migrate any external URL that is not already hosted on our S3 bucket
    private boolean isExternalUrl(String url) {
        return url != null && !url.contains("amazonaws.com");
    }
}
