package com.techstore.controller;

import com.techstore.dto.request.ReviewRequestDto;
import com.techstore.dto.response.ReviewResponseDto;
import com.techstore.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ── User endpoints ────────────────────────────────────────────────────────

    @PostMapping("/api/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponseDto> createReview(@Valid @RequestBody ReviewRequestDto dto) {
        return ResponseEntity.ok(reviewService.createReview(dto));
    }

    @GetMapping("/api/reviews/product/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getApprovedReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getApprovedReviewsForProduct(productId));
    }

    // ── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ReviewResponseDto>> getAdminReviews(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(reviewService.getReviewsForAdmin(status, pageable));
    }

    @PutMapping("/api/admin/reviews/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReviewResponseDto> approveReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PutMapping("/api/admin/reviews/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ReviewResponseDto> rejectReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
