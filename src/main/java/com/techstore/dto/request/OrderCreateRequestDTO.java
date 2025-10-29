package com.techstore.dto.request;

import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ShippingMethod;
import jakarta.persistence.Column;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateRequestDTO {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequestDTO> items;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String customerFirstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String customerLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 200)
    private String customerEmail;

    @NotBlank(message = "Phone is required")
    @Size(max = 20)
    private String customerPhone;

    @Size(max = 200)
    private String customerCompany;

    @Size(max = 50)
    private String customerVatNumber;

    private Boolean customerVatRegistered = false;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    @Size(max = 100)
    private String shippingCity;

    @Size(max = 20)
    private String shippingPostalCode;

    @NotBlank(message = "Shipping country is required")
    @Size(max = 100)
    private String shippingCountry = "Bulgaria";

    private Boolean useSameAddressForBilling = true;

    private String billingAddress;

    @Size(max = 100)
    private String billingCity;

    @Size(max = 20)
    private String billingPostalCode;

    @Size(max = 100)
    private String billingCountry = "Bulgaria";

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Shipping cost is required")
    @DecimalMin(value = "0.0", message = "Shipping cost must be positive")
    private BigDecimal shippingCost;

    @Size(max = 1000)
    private String customerNotes;

    @Data
    public static class OrderItemRequestDTO {

        @NotNull(message = "Product ID is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 999, message = "Quantity cannot exceed 999")
        private Integer quantity;
    }

    @NotNull(message = "Payment method is required")
    private Boolean isToSpeedyOffice = true;

    @NotNull
    private Boolean insuranceOffer = false;

    @NotNull
    private Boolean installmentOffer = false;

    @NotNull
    private Boolean termsAgreed = false;

    private ShippingMethod shippingMethod;
    private Long shippingSpeedySiteId;
    private Long shippingSpeedyOfficeId;
    private String shippingSpeedySiteName;
    private String shippingSpeedyOfficeName;

    private String password;
    private String confirmPassword;
}