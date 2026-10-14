package com.techstore.controller;

import com.techstore.dto.request.BlogCategoryRequestDto;
import com.techstore.dto.response.BlogCategoryResponseDto;
import com.techstore.service.BlogCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BlogCategoryController {

    private final BlogCategoryService blogCategoryService;

    // ── Public endpoints ─────────────────────────────────────────────────────

    @GetMapping("/api/blog/categories")
    public ResponseEntity<List<BlogCategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(blogCategoryService.getAllCategories());
    }

    @GetMapping("/api/blog/categories/tree")
    public ResponseEntity<List<BlogCategoryResponseDto>> getCategoryTree() {
        return ResponseEntity.ok(blogCategoryService.getCategoryTree());
    }

    @GetMapping("/api/blog/categories/{slug}")
    public ResponseEntity<BlogCategoryResponseDto> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(blogCategoryService.getBySlug(slug));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @PostMapping("/api/admin/blog/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogCategoryResponseDto> createCategory(
            @Valid @RequestBody BlogCategoryRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogCategoryService.createCategory(request));
    }

    @PutMapping("/api/admin/blog/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogCategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody BlogCategoryRequestDto request) {
        return ResponseEntity.ok(blogCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/api/admin/blog/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        blogCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
