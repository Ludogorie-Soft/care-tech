package com.techstore.dto.request;

import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentStatus;
import com.techstore.enums.ShippingMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFilterDTO {

    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private ShippingMethod shippingMethod;

    private LocalDate dateFrom;
    private LocalDate dateTo;

    private String customerEmail;
    private String customerName;
    private String orderNumber;

    // Sorting options
    private String sortBy = "createdAt"; // Default sort field
    private String sortDirection = "DESC"; // ASC or DESC
}