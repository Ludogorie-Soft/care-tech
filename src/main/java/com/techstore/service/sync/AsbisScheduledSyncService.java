package com.techstore.service.sync;

import com.techstore.service.AsbisApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled synchronization service for Asbis integration
 * Enable in application.properties:
 * asbis.sync.scheduled.enabled=true
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "asbis.sync.scheduled", name = "enabled", havingValue = "true")
public class AsbisScheduledSyncService {

    private final AsbisSyncService asbisSyncService;
    private final AsbisApiService asbisApiService;

    /**
     * Full sync every day at 3:00 AM
     * Cron format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "${asbis.sync.scheduled.cron:0 0 3 * * *}")
    public void scheduledFullSync() {
        log.info("=== Starting scheduled Asbis full synchronization ===");

        try {
            // Clear cache before sync
            asbisApiService.clearCache();

            // Test connection first
            if (!asbisApiService.testConnection()) {
                log.error("Asbis API connection test failed - skipping scheduled sync");
                return;
            }

            // Perform full sync
            long startTime = System.currentTimeMillis();

            log.info("Step 1/4: Syncing Asbis categories");
            asbisSyncService.syncAsbisCategories();

            log.info("Step 2/4: Syncing Asbis manufacturers");
            asbisSyncService.syncAsbisManufacturers();

            log.info("Step 3/4: Syncing Asbis parameters");
            asbisSyncService.syncAsbisParameters();

            log.info("Step 4/4: Syncing Asbis products");
            asbisSyncService.syncAsbisProducts();

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Scheduled Asbis full sync completed successfully in {}ms ===", duration);

        } catch (Exception e) {
            log.error("Error during scheduled Asbis synchronization", e);
        }
    }

    /**
     * Products only sync every 6 hours
     * Useful for keeping prices and availability up to date
     */
    @Scheduled(cron = "${asbis.sync.scheduled.products-cron:0 0 */6 * * *}")
    @ConditionalOnProperty(prefix = "asbis.sync.scheduled", name = "products-only-enabled", havingValue = "true")
    public void scheduledProductsSync() {
        log.info("=== Starting scheduled Asbis products synchronization ===");

        try {
            // Clear cache before sync
            asbisApiService.clearCache();

            // Test connection first
            if (!asbisApiService.testConnection()) {
                log.error("Asbis API connection test failed - skipping scheduled products sync");
                return;
            }

            long startTime = System.currentTimeMillis();
            asbisSyncService.syncAsbisProducts();
            long duration = System.currentTimeMillis() - startTime;

            log.info("=== Scheduled Asbis products sync completed in {}ms ===", duration);

        } catch (Exception e) {
            log.error("Error during scheduled Asbis products synchronization", e);
        }
    }

    /**
     * Cache cleanup every hour
     * Ensures fresh data on next sync
     */
    @Scheduled(cron = "0 0 * * * *")
    @ConditionalOnProperty(prefix = "asbis.sync.scheduled", name = "cache-cleanup-enabled", havingValue = "true", matchIfMissing = true)
    public void scheduledCacheCleanup() {
        log.debug("Clearing Asbis API cache (scheduled cleanup)");
        asbisApiService.clearCache();
    }
}