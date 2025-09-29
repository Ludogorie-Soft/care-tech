package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
    private List<ProductSearchResult> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private Map<String, List<FacetValue>> facets;
    private List<String> suggestions;
    private long searchTime;
}
