package com.techstore.service;

import com.techstore.dto.asbis.*;
import com.techstore.entity.SyncLog;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsbisStatsService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ManufacturerRepository manufacturerRepository;
    private final ParameterRepository parameterRepository;
    private final SyncLogRepository syncLogRepository;
    private final AsbisApiService asbisApiService;

    @Value("${asbis.api.enabled:false}")
    private boolean asbisApiEnabled;

    @Value("${asbis.api.base-url}")
    private String baseUrl;

    /**
     * Get comprehensive Asbis sync status
     */
    public AsbisSyncStatusDto getAsbisSyncStatus() {
        try {
            AsbisSyncStats stats = getAsbisSyncStats();
            List<AsbisSyncLogDto> recentLogs = getRecentSyncLogs(10);
            boolean connected = asbisApiService.testConnection();
            LocalDateTime lastSyncTime = getLastSyncTime();

            return AsbisSyncStatusDto.builder()
                    .enabled(asbisApiEnabled)
                    .connected(connected)
                    .lastSyncTime(lastSyncTime)
                    .stats(stats)
                    .recentLogs(recentLogs)
                    .build();

        } catch (Exception e) {
            log.error("Error getting Asbis sync status", e);
            return AsbisSyncStatusDto.builder()
                    .enabled(asbisApiEnabled)
                    .connected(false)
                    .build();
        }
    }

    /**
     * Get Asbis statistics
     */
    public AsbisSyncStats getAsbisSyncStats() {
        try {
            // Database stats
            Long totalProducts = productRepository.count();
            Long totalCategories = categoryRepository.count();
            Long totalManufacturers = manufacturerRepository.count();
            Long totalParameters = parameterRepository.count();

            Long asbisProducts = productRepository.countAsbisProducts();
            Long asbisCategories = categoryRepository.countAsbisCategories();
            Long asbisManufacturers = manufacturerRepository.countAsbisManufacturers();
            Long asbisParameters = parameterRepository.countAsbisParameters();

            // API stats
            Map<String, Object> apiStats = new HashMap<>();
            try {
                List<Map<String, Object>> apiProducts = asbisApiService.getAllProducts();
                List<Map<String, Object>> apiCategories = asbisApiService.extractCategories();
                Set<String> apiManufacturers = asbisApiService.extractManufacturers();
                Map<String, Set<String>> apiParameters = asbisApiService.extractParameters();

                apiStats.put("availableProducts", apiProducts.size());
                apiStats.put("availableCategories", apiCategories.size());
                apiStats.put("availableManufacturers", apiManufacturers.size());
                apiStats.put("availableParameters", apiParameters.size());
            } catch (Exception e) {
                log.warn("Could not fetch API stats: {}", e.getMessage());
                apiStats.put("error", "Could not connect to API");
            }

            return AsbisSyncStats.builder()
                    .totalProducts(totalProducts)
                    .totalCategories(totalCategories)
                    .totalManufacturers(totalManufacturers)
                    .totalParameters(totalParameters)
                    .asbisProducts(asbisProducts)
                    .asbisCategories(asbisCategories)
                    .asbisManufacturers(asbisManufacturers)
                    .asbisParameters(asbisParameters)
                    .apiStats(apiStats)
                    .build();

        } catch (Exception e) {
            log.error("Error getting Asbis stats", e);
            return AsbisSyncStats.builder().build();
        }
    }

    /**
     * Get recent sync logs
     */
    public List<AsbisSyncLogDto> getRecentSyncLogs(int limit) {
        try {
            PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

            List<SyncLog> logs = syncLogRepository.findAll(pageRequest)
                    .stream()
                    .filter(log -> log.getSyncType() != null && log.getSyncType().startsWith("ASBIS"))
                    .collect(Collectors.toList());

            return logs.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting recent sync logs", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get API information
     */
    public AsbisApiInfoDto getApiInfo() {
        try {
            boolean connected = asbisApiService.testConnection();

            Integer availableProducts = null;
            Integer availableCategories = null;
            Integer availableManufacturers = null;

            if (connected) {
                try {
                    availableProducts = asbisApiService.getAllProducts().size();
                    availableCategories = asbisApiService.extractCategories().size();
                    availableManufacturers = asbisApiService.extractManufacturers().size();
                } catch (Exception e) {
                    log.warn("Could not fetch API data: {}", e.getMessage());
                }
            }

            return AsbisApiInfoDto.builder()
                    .enabled(asbisApiEnabled)
                    .baseUrl(baseUrl)
                    .connected(connected)
                    .cacheTimeoutMinutes(5)
                    .lastCacheRefresh(LocalDateTime.now())
                    .availableProducts(availableProducts)
                    .availableCategories(availableCategories)
                    .availableManufacturers(availableManufacturers)
                    .build();

        } catch (Exception e) {
            log.error("Error getting API info", e);
            return AsbisApiInfoDto.builder()
                    .enabled(asbisApiEnabled)
                    .baseUrl(baseUrl)
                    .connected(false)
                    .build();
        }
    }

    /**
     * Get detailed sync result for a specific sync type
     */
    public Optional<AsbisSyncResultDto> getLastSyncResult(String syncType) {
        try {
            Optional<SyncLog> lastLog = syncLogRepository.findAll()
                    .stream()
                    .filter(log -> syncType.equals(log.getSyncType()))
                    .max(Comparator.comparing(SyncLog::getCreatedAt));

            return lastLog.map(this::convertToResultDto);

        } catch (Exception e) {
            log.error("Error getting last sync result for type: {}", syncType, e);
            return Optional.empty();
        }
    }

    /**
     * Get all available Asbis categories from API
     */
    public List<AsbisCategoryDto> getAvailableCategories() {
        try {
            List<Map<String, Object>> apiCategories = asbisApiService.extractCategories();

            return apiCategories.stream()
                    .map(cat -> {
                        String categoryId = getString(cat, "id");
                        String categoryName = getString(cat, "name");

                        // Check if synced
                        boolean synced = categoryRepository.findByAsbisId(categoryId).isPresent();

                        return AsbisCategoryDto.builder()
                                .asbisId(categoryId)
                                .asbisCode(categoryId)
                                .name(categoryName)
                                .synced(synced)
                                .build();
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting available categories", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all available Asbis manufacturers from API
     */
    public List<AsbisManufacturerDto> getAvailableManufacturers() {
        try {
            Set<String> apiManufacturers = asbisApiService.extractManufacturers();

            return apiManufacturers.stream()
                    .map(name -> {
                        // Check if synced
                        boolean synced = manufacturerRepository.findAll()
                                .stream()
                                .anyMatch(m -> name.equalsIgnoreCase(m.getName()));

                        return AsbisManufacturerDto.builder()
                                .name(name)
                                .asbisCode(name)
                                .synced(synced)
                                .build();
                    })
                    .sorted(Comparator.comparing(AsbisManufacturerDto::getName))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting available manufacturers", e);
            return new ArrayList<>();
        }
    }

    /**
     * Check for data inconsistencies
     */
    public Map<String, Object> checkDataIntegrity() {
        Map<String, Object> issues = new HashMap<>();

        try {
            // Check for duplicate products
            List<Object[]> duplicates = productRepository.findDuplicateProductsByAsbisCode();
            if (!duplicates.isEmpty()) {
                issues.put("duplicateProducts", duplicates.size());
            }

            // Check for products without categories
            long productsWithoutCategory = productRepository.findAllAsbisProducts()
                    .stream()
                    .filter(p -> p.getCategory() == null)
                    .count();
            if (productsWithoutCategory > 0) {
                issues.put("productsWithoutCategory", productsWithoutCategory);
            }

            // Check for products without manufacturer
            long productsWithoutManufacturer = productRepository.findAllAsbisProducts()
                    .stream()
                    .filter(p -> p.getManufacturer() == null)
                    .count();
            if (productsWithoutManufacturer > 0) {
                issues.put("productsWithoutManufacturer", productsWithoutManufacturer);
            }

            // Check for products without prices
            long productsWithoutPrice = productRepository.findAllAsbisProducts()
                    .stream()
                    .filter(p -> p.getPriceClient() == null)
                    .count();
            if (productsWithoutPrice > 0) {
                issues.put("productsWithoutPrice", productsWithoutPrice);
            }

            if (issues.isEmpty()) {
                issues.put("status", "OK");
                issues.put("message", "No data integrity issues found");
            } else {
                issues.put("status", "ISSUES_FOUND");
            }

        } catch (Exception e) {
            log.error("Error checking data integrity", e);
            issues.put("status", "ERROR");
            issues.put("error", e.getMessage());
        }

        return issues;
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private LocalDateTime getLastSyncTime() {
        try {
            return syncLogRepository.findAll()
                    .stream()
                    .filter(log -> log.getSyncType() != null && log.getSyncType().startsWith("ASBIS"))
                    .map(SyncLog::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private AsbisSyncLogDto convertToDto(SyncLog syncLog) {
        return AsbisSyncLogDto.builder()
                .id(syncLog.getId())
                .syncType(syncLog.getSyncType())
                .status(syncLog.getStatus())
                .recordsProcessed(syncLog.getRecordsProcessed())
                .recordsCreated(syncLog.getRecordsCreated())
                .recordsUpdated(syncLog.getRecordsUpdated())
                .errorMessage(syncLog.getErrorMessage())
                .durationMs(syncLog.getDurationMs())
                .createdAt(syncLog.getCreatedAt())
                .build();
    }

    private AsbisSyncResultDto convertToResultDto(SyncLog syncLog) {
        return AsbisSyncResultDto.builder()
                .success("SUCCESS".equals(syncLog.getStatus()))
                .message(syncLog.getErrorMessage() != null ? syncLog.getErrorMessage() : "Sync completed")
                .totalProcessed(syncLog.getRecordsProcessed())
                .created(syncLog.getRecordsCreated())
                .updated(syncLog.getRecordsUpdated())
                .durationMs(syncLog.getDurationMs())
                .build();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }
}