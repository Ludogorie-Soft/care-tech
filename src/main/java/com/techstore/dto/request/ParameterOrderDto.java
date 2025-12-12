package com.techstore.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParameterOrderDto {

    @NotNull(message = "Parameter ID cannot be null")
    private Long parameterId;

    @NotNull(message = "Order cannot be null")
    @PositiveOrZero(message = "Order must be zero or positive")
    private Integer newOrder;
}