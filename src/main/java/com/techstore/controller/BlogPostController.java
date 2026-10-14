package com.techstore.controller;

import com.techstore.dto.request.BlogPostRequestDto;
import com.techstore.dto.response.BlogPostResponseDto;
import com.techstore.dto.response.BlogPostSummaryDto;
import com.techstore.enums.BlogPostStatus;
import com.techstore.service.BlogPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
public class BlogPostController {

    private final BlogPostService blogPostService;

    // ── Public endpoints ────────────────────────────────────────────────────

    @GetMapping("/api/blog")
    public ResponseEntity<Page<BlogPostSummaryDto>> getPublishedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag) {

        if (category != null && !category.isBlank() && tag != null && !tag.isBlank()) {
            throw new com.techstore.exception.BusinessLogicException("Не може да се филтрира едновременно по категория и таг");
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)));
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(blogPostService.getPublishedByCategory(category, pageable));
        }
        if (tag != null && !tag.isBlank()) {
            return ResponseEntity.ok(blogPostService.getPublishedByTag(tag, pageable));
        }
        return ResponseEntity.ok(blogPostService.getPublishedPosts(pageable));
    }

    @GetMapping("/api/blog/featured")
    public ResponseEntity<Page<BlogPostSummaryDto>> getFeaturedPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 20)));
        return ResponseEntity.ok(blogPostService.getFeaturedPosts(pageable));
    }

    @GetMapping("/api/blog/{slug}")
    public ResponseEntity<BlogPostResponseDto> getPostBySlug(
            @PathVariable @Size(max = 255) String slug) {
        return ResponseEntity.ok(blogPostService.getBySlug(slug));
    }

    @PostMapping("/api/blog/{id}/view")
    public ResponseEntity<Void> incrementView(@PathVariable Long id) {
        blogPostService.incrementViewCount(id);
        return ResponseEntity.noContent().build();
    }

    // ── Admin endpoints ─────────────────────────────────────────────────────

    @GetMapping("/api/admin/blog")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<BlogPostSummaryDto>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending());
        BlogPostStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = BlogPostStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(blogPostService.getAllPosts(pageable, statusEnum, search));
    }

    @GetMapping("/api/admin/blog/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogPostResponseDto> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(blogPostService.getById(id));
    }

    @PostMapping("/api/admin/blog")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogPostResponseDto> createPost(
            @Valid @RequestBody BlogPostRequestDto request) {

        BlogPostResponseDto created = blogPostService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/admin/blog/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogPostResponseDto> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody BlogPostRequestDto request) {

        return ResponseEntity.ok(blogPostService.updatePost(id, request));
    }

    @DeleteMapping("/api/admin/blog/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        blogPostService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
