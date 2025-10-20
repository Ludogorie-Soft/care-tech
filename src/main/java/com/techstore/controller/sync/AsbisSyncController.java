package com.techstore.controller.sync;

import com.techstore.dto.asbis.*;
import com.techstore.service.AsbisApiService;
import com.techstore.service.AsbisStatsService;
import com.techstore.service.sync.AsbisSyncService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Hidden
@RestController
@RequestMapping("/api/sync/asbis")
@RequiredArgsConstructor
@Slf4j
public class AsbisSyncController {

    private final AsbisSyncService asbisSyncService;
    private final AsbisApiService asbisApiService;
    private final AsbisStatsService asbisStatsService;

    // ==========================================
    // STATUS & MONITORING ENDPOINTS
    // ==========================================

    /**
     * Get comprehensive Asbis sync status
     * GET /api/sync/asbis/status
     */
    @GetMapping("/status")
    public ResponseEntity<AsbisSyncStatusDto> getStatus() {
        log.info("Getting Asbis sync status");
        AsbisSyncStatusDto status = asbisStatsService.getAsbisSyncStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Get detailed statistics
     * GET /api/sync/asbis/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AsbisSyncStats> getStatistics() {
        log.info("Getting Asbis statistics");
        AsbisSyncStats stats = asbisStatsService.getAsbisSyncStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get API information
     * GET /api/sync/asbis/api-info
     */
    @GetMapping("/api-info")
    public ResponseEntity<AsbisApiInfoDto> getApiInfo() {
        log.info("Getting Asbis API info");
        AsbisApiInfoDto apiInfo = asbisStatsService.getApiInfo();
        return ResponseEntity.ok(apiInfo);
    }

    /**
     * Get recent sync logs
     * GET /api/sync/asbis/logs?limit=20
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AsbisSyncLogDto>> getRecentLogs(
            @RequestParam(defaultValue = "20") int limit) {
        log.info("Getting {} recent Asbis sync logs", limit);
        List<AsbisSyncLogDto> logs = asbisStatsService.getRecentSyncLogs(limit);
        return ResponseEntity.ok(logs);
    }

    /**
     * Check data integrity
     * GET /api/sync/asbis/integrity-check
     */
    @GetMapping("/integrity-check")
    public ResponseEntity<Map<String, Object>> checkDataIntegrity() {
        log.info("Checking Asbis data integrity");
        Map<String, Object> result = asbisStatsService.checkDataIntegrity();
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // DISCOVERY ENDPOINTS
    // ==========================================

    /**
     * Get available categories from Asbis API
     * GET /api/sync/asbis/available-categories
     */
    @GetMapping("/available-categories")
    public ResponseEntity<List<AsbisCategoryDto>> getAvailableCategories() {
        log.info("Getting available Asbis categories");
        List<AsbisCategoryDto> categories = asbisStatsService.getAvailableCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get available manufacturers from Asbis API
     * GET /api/sync/asbis/available-manufacturers
     */
    @GetMapping("/available-manufacturers")
    public ResponseEntity<List<AsbisManufacturerDto>> getAvailableManufacturers() {
        log.info("Getting available Asbis manufacturers");
        List<AsbisManufacturerDto> manufacturers = asbisStatsService.getAvailableManufacturers();
        return ResponseEntity.ok(manufacturers);
    }

    // ==========================================
    // SYNC ENDPOINTS
    // ==========================================

    /**
     * Test connection (lightweight)
     * GET /api/sync/asbis/test-connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        log.info("Testing Asbis API connection");

        Map<String, Object> response = new HashMap<>();
        try {
            boolean isConnected = asbisApiService.testConnection();
            response.put("success", isConnected);
            response.put("message", isConnected ? "Connection successful" : "Connection failed");

            if (isConnected) {
                List<Map<String, Object>> products = asbisApiService.getAllProducts();
                response.put("productCount", products.size());

                // Get detailed category statistics
                Map<String, Object> categoryStats = asbisApiService.getCategoryStatistics();
                response.put("categoryStatistics", categoryStats);

                Set<String> manufacturers = asbisApiService.extractManufacturers();
                response.put("manufacturerCount", manufacturers.size());

                // Add API stats for backward compatibility
                Map<String, Object> apiStats = new HashMap<>();
                apiStats.put("availableProducts", products.size());
                apiStats.put("availableCategories", categoryStats.get("total"));
                apiStats.put("availableManufacturers", manufacturers.size());
                apiStats.put("categoryBreakdown", categoryStats.get("breakdown"));
                response.put("apiStats", apiStats);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing Asbis API connection", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }


    /**
     * Sync categories
     * POST /api/sync/asbis/categories
     */
    @PostMapping("/categories")
    public ResponseEntity<AsbisSyncResultDto> syncCategories() {
        log.info("Starting Asbis categories synchronization via API");

        try {
            long startTime = System.currentTimeMillis();
            asbisSyncService.syncAsbisCategories();
            long duration = System.currentTimeMillis() - startTime;

            AsbisSyncResultDto result = asbisStatsService
                    .getLastSyncResult("ASBIS_CATEGORIES")
                    .orElse(AsbisSyncResultDto.builder()
                            .success(true)
                            .message("Sync completed")
                            .durationMs(duration)
                            .build());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing Asbis categories", e);
            return ResponseEntity.ok(AsbisSyncResultDto.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Sync manufacturers
     * POST /api/sync/asbis/manufacturers
     */
    @PostMapping("/manufacturers")
    public ResponseEntity<AsbisSyncResultDto> syncManufacturers() {
        log.info("Starting Asbis manufacturers synchronization via API");

        try {
            long startTime = System.currentTimeMillis();
            asbisSyncService.syncAsbisManufacturers();
            long duration = System.currentTimeMillis() - startTime;

            AsbisSyncResultDto result = asbisStatsService
                    .getLastSyncResult("ASBIS_MANUFACTURERS")
                    .orElse(AsbisSyncResultDto.builder()
                            .success(true)
                            .message("Sync completed")
                            .durationMs(duration)
                            .build());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing Asbis manufacturers", e);
            return ResponseEntity.ok(AsbisSyncResultDto.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Sync parameters
     * POST /api/sync/asbis/parameters
     */
    @PostMapping("/parameters")
    public ResponseEntity<AsbisSyncResultDto> syncParameters() {
        log.info("Starting Asbis parameters synchronization via API");

        try {
            long startTime = System.currentTimeMillis();
            asbisSyncService.syncAsbisParameters();
            long duration = System.currentTimeMillis() - startTime;

            AsbisSyncResultDto result = asbisStatsService
                    .getLastSyncResult("ASBIS_PARAMETERS")
                    .orElse(AsbisSyncResultDto.builder()
                            .success(true)
                            .message("Sync completed")
                            .durationMs(duration)
                            .build());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing Asbis parameters", e);
            return ResponseEntity.ok(AsbisSyncResultDto.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Sync products
     * POST /api/sync/asbis/products
     */
    @PostMapping("/products")
    public ResponseEntity<AsbisSyncResultDto> syncProducts() {
        log.info("Starting Asbis products synchronization via API");

        try {
            long startTime = System.currentTimeMillis();
            asbisSyncService.syncAsbisProducts();
            long duration = System.currentTimeMillis() - startTime;

            AsbisSyncResultDto result = asbisStatsService
                    .getLastSyncResult("ASBIS_PRODUCTS")
                    .orElse(AsbisSyncResultDto.builder()
                            .success(true)
                            .message("Sync completed")
                            .durationMs(duration)
                            .build());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error syncing Asbis products", e);
            return ResponseEntity.ok(AsbisSyncResultDto.builder()
                    .success(false)
                    .message("Error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Full synchronization
     * POST /api/sync/asbis/full
     */
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> fullSync() {
        log.info("Starting full Asbis synchronization via API");

        Map<String, Object> response = new HashMap<>();
        Map<String, AsbisSyncResultDto> results = new HashMap<>();

        try {
            long totalStartTime = System.currentTimeMillis();

            // Clear cache before starting
            asbisApiService.clearCache();

            // 1. Sync Categories
            log.info("Step 1/4: Syncing Asbis categories");
            try {
                asbisSyncService.syncAsbisCategories();
                results.put("categories", asbisStatsService
                        .getLastSyncResult("ASBIS_CATEGORIES")
                        .orElse(AsbisSyncResultDto.builder().success(true).build()));
            } catch (Exception e) {
                log.error("Failed to sync Asbis categories", e);
                results.put("categories", AsbisSyncResultDto.builder()
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }

            // 2. Sync Manufacturers
            log.info("Step 2/4: Syncing Asbis manufacturers");
            try {
                asbisSyncService.syncAsbisManufacturers();
                results.put("manufacturers", asbisStatsService
                        .getLastSyncResult("ASBIS_MANUFACTURERS")
                        .orElse(AsbisSyncResultDto.builder().success(true).build()));
            } catch (Exception e) {
                log.error("Failed to sync Asbis manufacturers", e);
                results.put("manufacturers", AsbisSyncResultDto.builder()
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }

            // 3. Sync Parameters
            log.info("Step 3/4: Syncing Asbis parameters");
            try {
                asbisSyncService.syncAsbisParameters();
                results.put("parameters", asbisStatsService
                        .getLastSyncResult("ASBIS_PARAMETERS")
                        .orElse(AsbisSyncResultDto.builder().success(true).build()));
            } catch (Exception e) {
                log.error("Failed to sync Asbis parameters", e);
                results.put("parameters", AsbisSyncResultDto.builder()
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }

            // 4. Sync Products
            log.info("Step 4/4: Syncing Asbis products");
            try {
                asbisSyncService.syncAsbisProducts();
                results.put("products", asbisStatsService
                        .getLastSyncResult("ASBIS_PRODUCTS")
                        .orElse(AsbisSyncResultDto.builder().success(true).build()));
            } catch (Exception e) {
                log.error("Failed to sync Asbis products", e);
                results.put("products", AsbisSyncResultDto.builder()
                        .success(false)
                        .message(e.getMessage())
                        .build());
            }

            long totalDuration = System.currentTimeMillis() - totalStartTime;

            boolean allSuccess = results.values().stream()
                    .allMatch(AsbisSyncResultDto::getSuccess);

            response.put("success", allSuccess);
            response.put("message", allSuccess ?
                    "Full Asbis synchronization completed successfully" :
                    "Full Asbis synchronization completed with some errors");
            response.put("totalDurationMs", totalDuration);
            response.put("results", results);

            log.info("Full Asbis synchronization completed in {}ms", totalDuration);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during full Asbis synchronization", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            response.put("results", results);
            return ResponseEntity.ok(response);
        }
    }

    // ==========================================
    // UTILITY ENDPOINTS
    // ==========================================

    /**
     * Clear API cache
     * POST /api/sync/asbis/clear-cache
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        log.info("Clearing Asbis API cache");
        asbisApiService.clearCache();

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "Asbis API cache cleared successfully");

        return ResponseEntity.ok(response);
    }

    /**
     * Get raw XML for debugging
     * GET /api/sync/asbis/raw-xml
     */
    @GetMapping("/raw-xml")
    public ResponseEntity<String> getRawXml() {
        log.info("Fetching raw XML from Asbis API");
        try {
            String xml = asbisApiService.getRawProductListXML();
            return ResponseEntity.ok(xml);
        } catch (Exception e) {
            log.error("Error fetching raw XML", e);
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }
}