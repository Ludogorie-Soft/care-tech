package com.techstore.config;

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

    public BigDecimal calculateShippingCost(BigDecimal orderSubtotal) {
        if (orderSubtotal.compareTo(freeShippingThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return defaultShippingCost;
    }
}