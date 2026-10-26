package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManufacturerSummaryDto {
    private Long id;
    private String name;
    private Boolean isPromoActive;
    private BigDecimal promoDiscount;
    private LocalDateTime promoEndsAt;
}
