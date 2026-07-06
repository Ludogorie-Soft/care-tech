package com.techstore.controller;

import com.techstore.dto.pazaruvaj.PazaruvajFeedConfig;
import com.techstore.service.PazaruvajFeedService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Hidden
@RestController
@RequestMapping("/api/pazaruvaj")
@RequiredArgsConstructor
@Slf4j
public class PazaruvajFeedController {

    private final PazaruvajFeedService pazaruvajFeedService;

    @Value("${app.url:https://www.caretech.bg}")
    private String appUrl;

    /**
     * Public XML product feed consumed by pazaruvaj.com crawler (every ~12 h).
     * URL to register in the pazaruvaj.com merchant admin panel.
     */
    @GetMapping(value = "/feed.xml", produces = "text/xml;charset=UTF-8")
    public ResponseEntity<String> getFeed() {
        String feed = pazaruvajFeedService.getCachedFeed();

        if (feed == null) {
            // Feed not yet generated (app just started) — trigger generation and return 503
            log.warn("[Pazaruvaj] Feed requested before generation completed");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Feed is being generated. Please retry in a few minutes.");
        }

        return ResponseEntity.ok()
                .header("Content-Type", "text/xml;charset=UTF-8")
                .body(feed);
    }

    /**
     * Admin-only endpoint to force a feed refresh without waiting for the schedule.
     */
    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> refreshFeed() {
        log.info("[Pazaruvaj] Manual feed refresh triggered");
        pazaruvajFeedService.refreshFeed();
        return ResponseEntity.ok("Feed refreshed successfully");
    }

    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<PazaruvajFeedConfig> getConfig() {
        return ResponseEntity.ok(pazaruvajFeedService.getFeedConfig());
    }

    @PutMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> updateConfig(@RequestBody PazaruvajFeedConfig config) {
        pazaruvajFeedService.updateFeedConfig(config);
        pazaruvajFeedService.refreshFeed();
        return ResponseEntity.ok("Config updated and feed refreshed");
    }

    /**
     * On-demand export — admin can filter by type (ALL / CATEGORY / PRODUCTS) and format (XML / CSV).
     * Returns the file as a downloadable attachment.
     */
    @GetMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> generateFeed(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "XML") String format,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String productIds) {

        boolean csv = "CSV".equalsIgnoreCase(format);

        List<Long> ids = null;
        if ("PRODUCTS".equalsIgnoreCase(type)) {
            if (productIds == null || productIds.isBlank()) {
                return ResponseEntity.badRequest().body("productIds е задължителен");
            }
            ids = Arrays.stream(productIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } else if ("CATEGORY".equalsIgnoreCase(type) && categoryId == null) {
            return ResponseEntity.badRequest().body("categoryId е задължителен");
        }

        String content;
        String ext = csv ? ".csv" : ".xml";
        String contentType = csv ? "text/csv;charset=UTF-8" : "text/xml;charset=UTF-8";

        if (csv) {
            content = switch (type.toUpperCase()) {
                case "CATEGORY" -> pazaruvajFeedService.generateCsvForCategory(categoryId);
                case "PRODUCTS" -> pazaruvajFeedService.generateCsvForProducts(ids);
                default         -> pazaruvajFeedService.generateCsvForAll();
            };
        } else {
            content = switch (type.toUpperCase()) {
                case "CATEGORY" -> pazaruvajFeedService.generateFeedForCategory(categoryId);
                case "PRODUCTS" -> pazaruvajFeedService.generateFeedForProducts(ids);
                default         -> pazaruvajFeedService.generateFeedForAll();
            };
        }

        String filename = switch (type.toUpperCase()) {
            case "CATEGORY" -> "products-category-" + categoryId + ext;
            case "PRODUCTS" -> "products-selection" + ext;
            default         -> "products-all" + ext;
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", contentType)
                .body(content);
    }

    /**
     * Admin-only status endpoint — returns feed metadata for the admin panel.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("ready", pazaruvajFeedService.isFeedReady());
        status.put("productCount", pazaruvajFeedService.getProductCount());
        status.put("feedUrl", appUrl + "/api/pazaruvaj/feed.xml");

        var refreshedAt = pazaruvajFeedService.getLastRefreshedAt();
        status.put("lastRefreshedAt", refreshedAt != null
                ? refreshedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                : null);

        return ResponseEntity.ok(status);
    }
}
