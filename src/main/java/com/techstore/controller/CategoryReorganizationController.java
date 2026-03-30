package com.techstore.controller;

import com.techstore.service.CategoryReorganizationService;
import com.techstore.service.CategoryReorganizationService.ReorganizationResult;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CategoryReorganizationController
 *
 * REST endpoint за реорганизация на категории след Vali и Tekra sync
 */
@Hidden
@Slf4j
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Category Reorganization", description = "Реорганизация на категории след sync")
public class CategoryReorganizationController {

    private final CategoryReorganizationService reorganizationService;

    /**
     * Реорганизира всички категории според бизнес структурата
     *
     * ВАЖНО: Пускай този endpoint СЛЕД Vali и Tekra sync!
     */
    @PostMapping("/reorganize")
    @Operation(summary = "Reorganize categories",
            description = "Reorganizes all categories according to business requirements. Run AFTER Vali and Tekra sync.")
    public ResponseEntity<ReorganizationResponse> reorganizeCategories() {
        log.info("=== CATEGORY REORGANIZATION REQUEST RECEIVED ===");

        try {
            ReorganizationResult result = reorganizationService.reorganizeAllCategories();

            if (result.success) {
                return ResponseEntity.ok(new ReorganizationResponse(
                        true,
                        "Category reorganization completed successfully",
                        result.created,
                        result.updated,
                        result.moved,
                        result.durationMs
                ));
            } else {
                return ResponseEntity.internalServerError().body(new ReorganizationResponse(
                        false,
                        "Category reorganization failed",
                        0, 0, 0, 0
                ));
            }

        } catch (Exception e) {
            log.error("Error during category reorganization", e);
            return ResponseEntity.internalServerError().body(new ReorganizationResponse(
                    false,
                    "Error: " + e.getMessage(),
                    0, 0, 0, 0
            ));
        }
    }

    public static class ReorganizationResponse {
        public boolean success;
        public String message;
        public int categoriesCreated;
        public int categoriesUpdated;
        public int categoriesMoved;
        public long durationMs;

        public ReorganizationResponse(boolean success, String message,
                                      int categoriesCreated, int categoriesUpdated,
                                      int categoriesMoved, long durationMs) {
            this.success = success;
            this.message = message;
            this.categoriesCreated = categoriesCreated;
            this.categoriesUpdated = categoriesUpdated;
            this.categoriesMoved = categoriesMoved;
            this.durationMs = durationMs;
        }
    }
}