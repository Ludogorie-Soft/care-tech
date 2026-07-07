package com.techstore.dto.pazaruvaj;

import java.math.BigDecimal;

public interface PazaruvajProductProjection {
    Long getId();
    String getProductName();
    String getSlug();
    BigDecimal getFinalPrice();
    String getPrimaryImageUrl();
    String getBarcode();
    String getDescriptionBg();
    String getSku();
    String getManufacturerName();
    String getCategoryName();
    String getParentCategoryName();
}
