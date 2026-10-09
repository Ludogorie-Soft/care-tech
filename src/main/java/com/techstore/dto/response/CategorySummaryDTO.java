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
public class CategorySummaryDTO {
    private Long id;
    private String nameEn;
    private String nameBg;
    private String slug;
    private Boolean show;
    private CategorySummaryDTO parent;
    private Boolean isPromoActive;
    private BigDecimal promoDiscount;
    private LocalDateTime promoEndsAt;
}
