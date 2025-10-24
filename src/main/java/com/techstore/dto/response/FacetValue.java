package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacetValue {
    /**
     * The ID of the parameter option (for filtering)
     * Example: 12 (for "8GB" RAM option)
     */
    private Long id;

    /**
     * The display name of the option
     * Example: "8GB", "Black", "Intel Core i7"
     */
    private String value;

    /**
     * Number of products that have this option
     * Example: 45 products have "8GB" RAM
     */
    private Long count;

    /**
     * Whether this option is currently selected in the filter
     */
    private Boolean selected;
}