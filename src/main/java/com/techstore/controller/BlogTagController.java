package com.techstore.controller;

import com.techstore.dto.request.BlogTagRequestDto;
import com.techstore.dto.response.BlogTagResponseDto;
import com.techstore.service.BlogTagService;
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
public class BlogTagController {

    private final BlogTagService blogTagService;

    // ── Public endpoints ─────────────────────────────────────────────────────

    @GetMapping("/api/blog/tags")
    public ResponseEntity<List<BlogTagResponseDto>> getAllTags() {
        return ResponseEntity.ok(blogTagService.getAllTags());
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @PostMapping("/api/admin/blog/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogTagResponseDto> createTag(
            @Valid @RequestBody BlogTagRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blogTagService.createTag(request));
    }

    @PutMapping("/api/admin/blog/tags/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<BlogTagResponseDto> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody BlogTagRequestDto request) {
        return ResponseEntity.ok(blogTagService.updateTag(id, request));
    }

    @DeleteMapping("/api/admin/blog/tags/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        blogTagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
