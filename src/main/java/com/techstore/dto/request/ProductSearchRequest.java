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

    private Map<Long, List<Long>> filters;

    private Boolean featured;
    private Boolean onSale;
}