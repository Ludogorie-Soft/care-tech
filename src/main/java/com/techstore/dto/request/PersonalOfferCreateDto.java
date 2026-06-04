package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PersonalOfferCreateDto {

    @NotBlank
    private String title;

    private String message;

    @NotNull
    private List<OfferItemDto> items;

    private BigDecimal discountPercent;

    private LocalDateTime expiresAt;

    @Data
    public static class OfferItemDto {
        private Long productId;
        private String productName;
        private String imageUrl;
        private BigDecimal originalPrice;
        private BigDecimal offerPrice;
        private Integer quantity;
    }
}
