package com.techstore.controller.sync;

import com.techstore.service.AsbisApiService;
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

    private final AsbisApiService asbisApiService;
    private final AsbisSyncService asbisSyncService;

    /**
     * Test Asbis API connection
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
     * Get statistics about Asbis products
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Getting Asbis API statistics");

        Map<String, Object> response = new HashMap<>();

        try {
            List<Map<String, Object>> products = asbisApiService.getAllProducts();
            Set<String> manufacturers = asbisApiService.extractManufacturers();
            List<Map<String, Object>> categories = asbisApiService.extractCategories();
            Map<String, Set<String>> parameters = asbisApiService.extractParameters();

            response.put("success", true);
            response.put("productsCount", products.size());
            response.put("manufacturersCount", manufacturers.size());
            response.put("categoriesCount", categories.size());
            response.put("parametersCount", parameters.size());

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
        log.info("Starting Asbis manufacturers synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisSyncService.syncAsbisManufacturers();
            response.put("success", true);
            response.put("message", "Manufacturers synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Asbis manufacturers", e);
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
        log.info("Starting Asbis categories synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisSyncService.syncAsbisCategories();
            response.put("success", true);
            response.put("message", "Categories synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Asbis categories", e);
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
        log.info("Starting Asbis parameters synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisSyncService.syncAsbisParameters();
            response.put("success", true);
            response.put("message", "Parameters synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Asbis parameters", e);
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
        log.info("Starting Asbis products synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisSyncService.syncAsbisProducts();
            response.put("success", true);
            response.put("message", "Products synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Asbis products", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Synchronize prices and availability from PriceAvail.xml
     */
    @PostMapping("/price-avail")
    public ResponseEntity<Map<String, Object>> syncPriceAvail() {
        log.info("Starting Asbis price/availability synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisSyncService.syncAsbisPriceAvail();
            response.put("success", true);
            response.put("message", "Price/availability synchronized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error syncing Asbis price/availability", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Debug: return raw PriceAvail.xml content (first 5000 chars)
     */
    @GetMapping("/price-avail/raw")
    public ResponseEntity<String> getRawPriceAvailXml() {
        String xml = asbisApiService.getRawPriceAvailXML();
        String preview = xml != null && xml.length() > 5000 ? xml.substring(0, 5000) + "\n...[truncated]" : xml;
        return ResponseEntity.ok().header("Content-Type", "text/plain;charset=UTF-8").body(preview);
    }

    /**
     * Full synchronization (all data)
     */
    @PostMapping("/full")
    public ResponseEntity<Map<String, Object>> fullSync() {
        log.info("Starting full Asbis synchronization");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisApiService.clearCache();

            asbisSyncService.syncAsbisManufacturers();
            asbisSyncService.syncAsbisCategories();
            asbisSyncService.syncAsbisParameters();
            asbisSyncService.syncAsbisProducts();
            asbisSyncService.syncAsbisPriceAvail();

            response.put("success", true);
            response.put("message", "Full synchronization completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during full Asbis synchronization", e);
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
        log.info("Clearing Asbis API cache");

        Map<String, Object> response = new HashMap<>();

        try {
            asbisApiService.clearCache();
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
