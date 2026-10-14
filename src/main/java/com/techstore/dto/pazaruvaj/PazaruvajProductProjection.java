package com.techstore.dto.pazaruvaj;

import java.math.BigDecimal;

public interface PazaruvajProductProjection {
    Long getId();
    String getProductName();
    String getSlug();
    BigDecimal getFinalPrice();
    /** Kilograms; only VALI sends it, so it is null for most other products. */
    BigDecimal getWeight();
    Long getCategoryId();
    String getPrimaryImageUrl();
    String getBarcode();
    String getDescriptionBg();
    String getSku();
    String getManufacturerName();
    String getCategoryName();
    String getParentCategoryName();
}
