package com.techstore.dto.filter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Current selection on a category page. Prices are compared with products.final_price exactly as
 * {@code POST /api/products/search} does.
 *
 * @param attributeFilters filter attribute id -> selected filter value ids
 */
public record CategoryFiltersRequest(Map<Long, List<Long>> attributeFilters,
                                     List<Long> manufacturers,
                                     BigDecimal minPrice,
                                     BigDecimal maxPrice,
                                     String language) {
}
