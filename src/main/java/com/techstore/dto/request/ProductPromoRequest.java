package com.techstore.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductPromoRequest {
    private Long id;
    private BigDecimal discount;

    /** Optional — the discount is removed automatically at this moment (Europe/Sofia). */
    private LocalDateTime promoEndsAt;
}
