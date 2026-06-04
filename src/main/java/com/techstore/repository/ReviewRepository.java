package com.techstore.repository;

import com.techstore.entity.Review;
import com.techstore.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status);

    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.status = 'APPROVED'")
    Double findAverageRatingByProductId(@Param("productId") Long productId);

    long countByProductIdAndStatus(Long productId, ReviewStatus status);
}
