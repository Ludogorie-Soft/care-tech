package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single facet value with its count
 * Used for building filter UI with product counts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetValue {

    /**
     * The value/option name (e.g., "8GB", "Черен", "Intel Core i7")
     */
    private String value;

    /**
     * Number of products matching this facet value
     */
    private Long count;

    /**
     * Whether this facet value is currently selected in the search
     */
    private Boolean selected;
}