package com.techstore.controller.sync;

import com.techstore.service.MostApiService;
import com.techstore.service.sync.MostSyncService;
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
@RequestMapping("/api/sync/most")
@RequiredArgsConstructor
@Slf4j
public class MostSyncController {

    private final MostApiService mostApiService;
    private final MostSyncService mostSyncService;

    /**
     * Test Most API connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        log.info("Testing Most API connection");

        Map<String, Object> response = new HashMap<>();

        try {
            boolean isConnected = mostApiService.testConnection();
            response.put("success", isConnected);
            response.put("message", isConnected ? "Connection successful" : "Connection failed");

            if (isConnected) {
                List<Map<String, Object>> products = mostApiService.getAllProducts();
                response.put("productsCount", products.size());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get statistics about Most products
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Getting Most API statistics");

        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> products = mostApiService.getAllProducts();
            Set<String> manufacturers = mostApiService.extractUniqueManufacturers();
            Set<String> categories = mostApiService.extractUniqueCategories();
            Map<String, Set<String>> parameters = mostApiService.extractUniqueParameters();

            response.put("success", true);
            response.put("productsCount", products.size());
            response.put("manufacturersCount", manufacturers.size());
            response.put("categoriesCount", categories.size());
            response.put("parametersCount", parameters.size());

            // Sample data
            if (!products.isEmpty()) {
                response.put("sampleProduct", products.get(0));
            }
            response.put("manufacturers", manufacturers);
            response.put("categories", categories);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronize manufacturers
     */
    @PostMapping("/manufacturers")
    public ResponseEntity<Map<String, Object>> syncManufacturers() {
        log.info("Starting Most manufacturers synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            mostSyncService.syncMostManufacturers();
            response.put("success", true);
            response.put("message", "Manufacturers synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Most manufacturers", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronize categories
     */
    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> syncCategories() {
        log.info("Starting Most categories synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            mostSyncService.syncMostCategories();
            response.put("success", true);
            response.put("message", "Categories synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Most categories", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronize parameters
     */
    @PostMapping("/parameters")
    public ResponseEntity<Map<String, Object>> syncParameters() {
        log.info("Starting Most parameters synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            mostSyncService.syncMostParameters();
            response.put("success", true);
            response.put("message", "Parameters synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Most parameters", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronize products
     */
    @PostMapping("/products")
    public ResponseEntity<Map<String, Object>> syncProducts() {
        log.info("Starting Most products synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            mostSyncService.syncMostProducts();
            response.put("success", true);
            response.put("message", "Products synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Most products", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Full synchronization (all data)
     */
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> fullSync() {
        log.info("Starting full Most synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            // Clear cache before full sync
            mostApiService.clearCache();

            // Sync in order
            mostSyncService.syncMostManufacturers();
            mostSyncService.syncMostCategories();
            mostSyncService.syncMostParameters();
            mostSyncService.syncMostProducts();

            response.put("success", true);
            response.put("message", "Full synchronization completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during full Most synchronization", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Clear cache
     */
    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, Object>> clearCache() {
        log.info("Clearing Most API cache");

        Map<String, Object> response = new HashMap<>();

        try {
            mostApiService.clearCache();
            response.put("success", true);
            response.put("message", "Cache cleared successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}