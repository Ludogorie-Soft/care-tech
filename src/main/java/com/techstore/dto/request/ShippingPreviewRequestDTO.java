package com.techstore.dto.request;

import com.techstore.enums.ShippingMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShippingPreviewRequestDTO {

    private ShippingMethod shippingMethod;
    private Boolean isToSpeedyOffice;
    private Long shippingSpeedySiteId; // За доставка до адрес
    private BigDecimal subtotal; // Сума на продуктите

    // За по-точно изчисление
    private List<CartItemDTO> items;

    @Data
    public static class CartItemDTO {
        private Long productId;
        private Integer quantity;
        private BigDecimal weight; // Опционално, ако имаш weight
    }
}