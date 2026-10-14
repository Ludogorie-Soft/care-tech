package com.techstore.dto.tbi;

import lombok.Data;

/**
 * A single cart item sent from the frontend when initiating a leasing application
 * from the cart page (multiple products).
 */
@Data
public class TbiCartItemRequestDto {

    private Long   productId;
    private String productName;
    private Integer quantity;

    /** Unit price in EUR with VAT */
    private Double priceEuro;

    private String sku;
    private String imageUrl;
}
