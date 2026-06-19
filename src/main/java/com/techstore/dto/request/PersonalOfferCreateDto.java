package com.techstore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
    @Valid
    private List<OfferItemDto> items;

    private BigDecimal discountPercent;

    private LocalDateTime expiresAt;

    @Data
    public static class OfferItemDto {
        private Long productId;
        private String productName;
        private String imageUrl;
        private BigDecimal originalPrice;

        @NotNull(message = "Offer price is required")
        @DecimalMin(value = "0.01", message = "Offer price must be greater than zero")
        private BigDecimal offerPrice;

        private BigDecimal discountPercent;
        private Integer quantity;
    }
}
