package com.techstore.dto.request;

import jakarta.validation.constraints.DecimalMax;
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
public class ProductMarkupUpdateDTO {

    @NotNull(message = "Markup percentage is required")
    @DecimalMin(value = "0.00", message = "Markup percentage cannot be negative")
    private BigDecimal markupPercentage;
}