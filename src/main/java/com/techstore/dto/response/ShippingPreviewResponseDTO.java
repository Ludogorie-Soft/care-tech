package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingPreviewResponseDTO {

    private BigDecimal shippingCost;
    private Boolean isFreeShipping;
    private Boolean isCalculatedViaApi; // true ако е изчислено през Speedy API
    private String calculationMethod; // "SPEEDY_API", "STANDARD", "FREE"
    private String message; // За потребителя

    // Speedy specific details
    private BigDecimal speedyAmount;
    private BigDecimal speedyVat;
    private BigDecimal speedyTotal;
    private String speedyDeliveryDeadline;
}