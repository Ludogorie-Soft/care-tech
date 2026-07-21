package com.techstore.service;

import com.techstore.dto.request.ReviewRequestDto;
import com.techstore.dto.response.ReviewResponseDto;
import com.techstore.entity.Product;
import com.techstore.entity.Review;
import com.techstore.entity.User;
import com.techstore.enums.ReviewStatus;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.ReviewRepository;
import com.techstore.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final SecurityHelper securityHelper;

    @Transactional
    public ReviewResponseDto createReview(ReviewRequestDto dto) {
        User user = securityHelper.getCurrentUser();

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), dto.getProductId())) {
            throw new BusinessLogicException("Вече сте оставили отзив за този продукт.");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + dto.getProductId()));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review.setStatus(ReviewStatus.PENDING);

        return ReviewResponseDto.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> getApprovedReviewsForProduct(Long productId) {
        return reviewRepository
                .findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.APPROVED)
                .stream()
                .map(ReviewResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getReviewsForAdmin(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            ReviewStatus reviewStatus = ReviewStatus.valueOf(status.toUpperCase());
            return reviewRepository.findByStatusOrderByCreatedAtDesc(reviewStatus, pageable)
                    .map(ReviewResponseDto::from);
        }
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ReviewResponseDto::from);
    }

    @Transactional
    public ReviewResponseDto approveReview(Long id) {
        Review review = findOrThrow(id);
        review.setStatus(ReviewStatus.APPROVED);
        return ReviewResponseDto.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponseDto rejectReview(Long id) {
        Review review = findOrThrow(id);
        review.setStatus(ReviewStatus.REJECTED);
        return ReviewResponseDto.from(reviewRepository.save(review));
    }

    @Transactional
    public void deleteReview(Long id) {
        reviewRepository.delete(findOrThrow(id));
    }

    private Review findOrThrow(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }
}
