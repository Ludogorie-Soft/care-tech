package com.techstore.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CartSummaryDto {
    private List<CartItemResponseDto> items;
    private Integer totalItems;
    private BigDecimal totalPrice;

    /** Admin only: items whose product can no longer be bought. Absent from the customer's own cart. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CartItemResponseDto> unavailableItems;
}
