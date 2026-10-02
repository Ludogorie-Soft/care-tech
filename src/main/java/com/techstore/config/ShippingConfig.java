package com.techstore.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * Delivery terms of the shop — the single source for the order total and the Pazaruvaj feed.
 * The frontend mirrors these values in {@code care-tech-ui/src/utils/utils.js}; change both together.
 */
@Configuration
@Getter
public class ShippingConfig {

    /** Price of a delivery to a Speedy office, EUR with VAT. */
    @Value("${shipping.cost.default:3.50}")
    private BigDecimal defaultShippingCost;

    /** Order value (EUR <b>with</b> VAT) from which delivery to a Speedy office is free. */
    @Value("${shipping.cost.free.threshold:128.00}")
    private BigDecimal freeShippingThreshold;

    /** Working days to deliver an in-stock order — shown on the product page and in the Pazaruvaj feed. */
    @Value("${shipping.delivery.days:2}")
    private int deliveryDays;

    public boolean isFreeShipping(BigDecimal grossAmount) {
        return grossAmount.compareTo(freeShippingThreshold) >= 0;
    }

    /**
     * Изчислява цената на доставка
     *
     * @param grossSubtotal - сума на продуктите с ДДС
     * @param isToSpeedyOffice - дали е доставка до офис или до адрес
     * @return цена на доставка (0 ако е до адрес, или според тарифа ако е до офис)
     */
    public BigDecimal calculateShippingCost(BigDecimal grossSubtotal, Boolean isToSpeedyOffice) {
        // Ако доставката е до адрес (не е до офис), цената е 0 (ще се начисли от куриера)
        if (Boolean.FALSE.equals(isToSpeedyOffice)) {
            return BigDecimal.ZERO;
        }

        // Ако е до офис, използваме стандартната логика
        if (isFreeShipping(grossSubtotal)) {
            return BigDecimal.ZERO; // Безплатна доставка над threshold
        }

        return defaultShippingCost; // Стандартна цена под threshold
    }
}
