package com.techstore.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkupUpdateDTO {

    @NotNull(message = "Markup percentage is required")
    @DecimalMin(value = "0.00", message = "Markup percentage cannot be negative")
    private BigDecimal markupPercentage;

    /**
     * If true, applies markup only to products that currently have markupPercentage = 0
     * If false, updates all products regardless of current markup value
     */
    private Boolean onlyZeroMarkup = false;

    /**
     * If true, only active products will be updated
     * If false, all products (active and inactive) will be updated
     */
    private Boolean onlyActive = true;
}