package com.techstore.dto.response;

import com.techstore.entity.Review;
import com.techstore.enums.ReviewStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponseDto {

    private Long id;
    private Long productId;
    private Short rating;
    private String comment;
    private String authorName;
    private ReviewStatus status;
    private LocalDateTime createdAt;

    public static ReviewResponseDto from(Review review) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.id = review.getId();
        dto.productId = review.getProduct().getId();
        dto.rating = review.getRating();
        dto.comment = review.getComment();
        dto.status = review.getStatus();
        dto.createdAt = review.getCreatedAt();

        String first = review.getUser().getFirstName();
        String last = review.getUser().getLastName();
        if (first != null && last != null) {
            dto.authorName = first + " " + last.charAt(0) + ".";
        } else if (first != null) {
            dto.authorName = first;
        } else {
            dto.authorName = review.getUser().getEmail().split("@")[0];
        }

        return dto;
    }
}
