package com.techstore.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Product.calculateFinalPrice — надценка и отстъпка")
class ProductFinalPriceTest {

    private Product product(String priceClient, String markup, String discount) {
        Product p = new Product();
        p.setPriceClient(new BigDecimal(priceClient));
        p.setMarkupPercentage(new BigDecimal(markup));
        p.setDiscount(new BigDecimal(discount));
        p.calculateFinalPrice();
        return p;
    }

    @Test
    @DisplayName("Дробна надценка не се закръгля до цял процент (12.5% остава 12.5%)")
    void fractionalMarkupIsKeptExactly() {
        assertEquals(0, new BigDecimal("112.50").compareTo(product("100.00", "12.5", "0").getFinalPrice()));
    }

    @Test
    @DisplayName("Цяла надценка и отстъпка — без промяна спрямо досега")
    void wholeMarkupWithDiscount() {
        assertEquals(0, new BigDecimal("125.00").compareTo(product("100.00", "25", "0").getFinalPrice()));
        assertEquals(new BigDecimal("112.50"), product("100.00", "25", "10").getFinalPrice());
    }
}
