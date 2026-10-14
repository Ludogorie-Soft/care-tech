package com.techstore.dto.tbi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * A single item in the TBI RegisterApplication items list.
 * Field names match exactly what the TBI API expects.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TbiItemDto {

    private String name;
    private String description;

    /** Quantity as string — TBI API expects String */
    private String qty;

    /** Unit price in EUR as string */
    private String price;

    private String sku;

    /**
     * TBI product category ID.
     * Use 0 for general / unspecified category.
     * Certain promotional schemes are restricted to specific categories.
     */
    private String category;

    private String imagelink;
}
