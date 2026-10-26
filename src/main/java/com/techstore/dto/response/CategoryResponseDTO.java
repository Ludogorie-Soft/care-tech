package com.techstore.dto.response;

import com.techstore.dto.response.CategorySummaryDTO;
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
public class CategoryResponseDTO {
    private Long id;
    private Long externalId;
    private String nameBg;
    private String nameEn;
    private String slug;
    private Integer sortOrder;
    private CategorySummaryDTO parent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isParentCategory;
    private Boolean show;
    private Boolean isPromoActive;
    /** Only in the admin "with markup" list: the most common markup among the products. */
    private BigDecimal markupPercentage;
}
