package com.techstore.dto.request;

import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ShippingMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConvertOfferToOrderRequestDto {

    @NotBlank(message = "Phone is required")
    private String customerPhone;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Shipping city is required")
    private String shippingCity;

    private String shippingPostalCode;

    private String shippingCountry = "Bulgaria";

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private ShippingMethod shippingMethod;

    private Boolean isToSpeedyOffice = false;

    private Long shippingSpeedySiteId;

    private Long shippingSpeedyOfficeId;

    private String shippingSpeedySiteName;

    private String shippingSpeedyOfficeName;

    private String customerNotes;

    private String customerCompany;
    private String customerVatNumber;
    private Boolean customerVatRegistered = false;
}
