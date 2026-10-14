package com.techstore.controller;

import com.techstore.service.filter.CategoryFilterService;
import com.techstore.service.filter.FilterIndexService;
import com.techstore.service.filter.FilterRebuildResult;
import com.techstore.service.filter.FilterReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Canonical filter index maintenance. A dry run executes the whole rebuild and rolls it back, which is
 * how the effect of new mapping rules is previewed before they reach shoppers.
 */
@RestController
@RequestMapping("/api/admin/filters")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class FilterAdminController {

    private final FilterIndexService filterIndexService;
    private final FilterReportService filterReportService;
    private final CategoryFilterService categoryFilterService;

    @PostMapping("/rebuild")
    public ResponseEntity<FilterRebuildResult> rebuild(@RequestParam(defaultValue = "false") boolean dryRun) {
        return ResponseEntity.ok(filterIndexService.rebuild("ADMIN", dryRun));
    }

    /** Shows or hides a filter group in a category; kept by every later rebuild. */
    @PutMapping("/categories/{categoryId}/attributes/{attributeId}/visibility")
    public ResponseEntity<Void> setVisibility(@PathVariable Long categoryId,
                                              @PathVariable Long attributeId,
                                              @RequestParam boolean visible) {
        categoryFilterService.setVisibility(categoryId, attributeId, visible);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> report() {
        return ResponseEntity.ok(filterReportService.report());
    }
}
