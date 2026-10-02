package com.techstore.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Until 2026-10-02 the cart showed free delivery from 250 лв. with VAT (≈ 128 €), while the
 * order was charged against 150 € without VAT (≈ 180 € with VAT) — a 140 € order to a Speedy
 * office saw "БЕЗПЛАТНО" and was billed 3.50 €. The threshold is now one amount, with VAT
 * (128 € at first, 170 € since 2026-10-02).
 */
class ShippingConfigTest {

    private ShippingConfig config;

    @BeforeEach
    void setUp() {
        config = new ShippingConfig();
        ReflectionTestUtils.setField(config, "defaultShippingCost", new BigDecimal("3.50"));
        ReflectionTestUtils.setField(config, "freeShippingThreshold", new BigDecimal("170.00"));
        ReflectionTestUtils.setField(config, "deliveryDays", 2);
    }

    @Test
    @DisplayName("Office delivery just under the threshold costs the flat rate")
    void officeUnderThresholdPays() {
        assertEquals(new BigDecimal("3.50"), config.calculateShippingCost(new BigDecimal("169.99"), true));
    }

    @Test
    @DisplayName("Office delivery at the threshold is free")
    void officeAtThresholdIsFree() {
        assertEquals(BigDecimal.ZERO, config.calculateShippingCost(new BigDecimal("170.00"), true));
    }

    @Test
    @DisplayName("Address delivery is not charged by the shop — the courier bills it")
    void addressIsChargedByCourier() {
        assertEquals(BigDecimal.ZERO, config.calculateShippingCost(new BigDecimal("50.00"), false));
    }

    @Test
    @DisplayName("isFreeShipping compares the amount with VAT against the threshold")
    void isFreeShippingBoundary() {
        assertFalse(config.isFreeShipping(new BigDecimal("169.99")));
        assertTrue(config.isFreeShipping(new BigDecimal("170.00")));
    }
}
