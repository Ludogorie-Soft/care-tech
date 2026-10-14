package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemUpdateDTO {

    @NotNull
    private Long orderItemId;

    /** 0 = remove the item from the order. */
    @NotNull
    @Min(0)
    private Integer quantity;
}
