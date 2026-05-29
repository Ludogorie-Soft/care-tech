package com.techstore.dto.tbi;

import com.techstore.enums.ShippingMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Sent by the frontend when the customer clicks "Buy with TBI".
 * Contains product details + shipping address collected before opening the TBI iframe.
 */
@Data
public class TbiRegisterRequestDto {

    /** Our internal product ID */
    private Long productId;

    private String productName;

    /** Price in EUR (with VAT) — TBI requires EUR */
    private Double priceEuro;

    /** Price in BGN (with VAT) — stored for our records */
    private Double priceLeva;

    private String sku;

    private Integer quantity;

    private String imageUrl;

    /**
     * Multiple cart items — when present, single-product fields above are ignored.
     * Used when initiating leasing from the cart page.
     */
    private List<TbiCartItemRequestDto> cartItems;

    // ── Shipping — collected from the customer before opening the TBI iframe ──

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    private String shippingCity;

    private String shippingPostalCode;

    private String shippingCountry = "Bulgaria";

    private ShippingMethod shippingMethod;

    private Boolean isToSpeedyOffice = false;

    private Long   shippingSpeedySiteId;
    private Long   shippingSpeedyOfficeId;
    private String shippingSpeedySiteName;
    private String shippingSpeedyOfficeName;
}
