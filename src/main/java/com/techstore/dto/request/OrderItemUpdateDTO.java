package com.techstore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemUpdateDTO {

    @NotNull
    private Long orderItemId;

    /** 0 = remove the item from the order. */
    @NotNull
    @Min(0)
    private Integer quantity;

    /** New unit price without VAT; null = keep the current price. */
    @DecimalMin(value = "0.01", message = "Unit price must be greater than zero")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal unitPrice;
}
