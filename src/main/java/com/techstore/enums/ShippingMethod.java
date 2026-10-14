package com.techstore.enums;

public enum ShippingMethod {
    SPEEDY("Speedy"),
    FREE("Безплатна доставка"),
    STANDARD("Стандартна доставка"),
    EXPRESS("Експресна доставка");

    private final String displayName;

    ShippingMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
