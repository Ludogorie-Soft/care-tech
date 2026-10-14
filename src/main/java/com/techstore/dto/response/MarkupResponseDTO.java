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
public class MarkupResponseDTO {

    private String operationType; // "CATEGORY" or "MANUFACTURER"

    private Long targetId;

    private String targetName;

    private BigDecimal markupPercentage;

    private Integer totalProducts;

    private Integer updatedProducts;

    private Integer skippedProducts;

    private LocalDateTime executedAt;

    private String message;
}