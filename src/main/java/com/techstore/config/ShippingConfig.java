package com.techstore.config;

import com.techstore.enums.ShippingMethod;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@Getter
public class ShippingConfig {

    @Value("${shipping.cost.default:5.90}")
    private BigDecimal defaultShippingCost;

    @Value("${shipping.cost.free.threshold:250.00}")
    private BigDecimal freeShippingThreshold;

    /**
     * Изчислява цената на доставка
     *
     * @param orderSubtotal - сума на продуктите
     * @param isToSpeedyOffice - дали е доставка до офис или до адрес
     * @return цена на доставка (0 ако е до адрес, или според тарифа ако е до офис)
     */
    public BigDecimal calculateShippingCost(BigDecimal orderSubtotal, Boolean isToSpeedyOffice) {
        // Ако доставката е до адрес (не е до офис), цената е 0 (ще се начисли от куриера)
        if (Boolean.FALSE.equals(isToSpeedyOffice)) {
            return BigDecimal.ZERO;
        }

        // Ако е до офис, използваме стандартната логика
        if (orderSubtotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO; // Безплатна доставка над threshold
        }

        return defaultShippingCost; // Стандартна цена под threshold
    }
}