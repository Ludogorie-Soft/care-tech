package com.techstore.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchRequest {
    private String query;
    private String language = "bg";
    private List<String> categories;
    private List<String> manufacturers;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;
    private Boolean active = true;

    @Builder.Default
    private String sortBy = "price_asc"; // relevance, price_asc, price_desc, name, newest

    private int page = 0;
    private int size = 20;

    /**
     * Parameter filtering by IDs: Map<parameterId, List<parameterOptionId>>
     *
     * Example:
     * {
     *   5: [12, 13],  // Parameter 5 (RAM) must be 8GB (12) OR 16GB (13)
     *   7: [25]       // AND Parameter 7 (Color) must be Black (25)
     * }
     *
     * This means:
     * - Products must have (RAM=8GB OR RAM=16GB) AND (Color=Black)
     * - Multiple options for same parameter = OR logic
     * - Multiple parameters = AND logic
     */
    private Map<Long, List<Long>> filters;

    private Boolean featured;
    private Boolean onSale;
}