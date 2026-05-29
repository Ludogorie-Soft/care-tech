package com.techstore.dto.tbi;

import com.techstore.entity.LeasingApplication;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-facing representation of a TBI leasing application.
 */
@Data
public class LeasingApplicationResponseDto {

    private Long id;
    private Long orderId;
    private String orderNumber;

    private Integer tbiOrderId;
    private String  tbiToken;
    private String  tbiApplicationId;
    private String  storeOrderId;

    private String     status;
    private BigDecimal amountEur;
    private BigDecimal installment;
    private BigDecimal downpayment;
    private Integer    periodMonths;

    private String customerFirstName;
    private String customerLastName;
    private String customerPhone;
    private String customerEmail;

    private Long   productId;
    private String productName;

    private String statusHistory;   // raw JSON string for the frontend to parse
    private String itemsJson;

    // Shipping
    private String  shippingAddress;
    private String  shippingCity;
    private String  shippingPostalCode;
    private Boolean isToSpeedyOffice;
    private Long    shippingSpeedySiteId;
    private Long    shippingSpeedyOfficeId;
    private String  shippingSpeedySiteName;
    private String  shippingSpeedyOfficeName;

    private LocalDateTime webhookReceivedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LeasingApplicationResponseDto from(LeasingApplication e) {
        LeasingApplicationResponseDto dto = new LeasingApplicationResponseDto();
        dto.setId(e.getId());
        dto.setTbiOrderId(e.getTbiOrderId());
        dto.setTbiToken(e.getTbiToken());
        dto.setTbiApplicationId(e.getTbiApplicationId());
        dto.setStoreOrderId(e.getStoreOrderId());
        dto.setStatus(e.getStatus());
        dto.setAmountEur(e.getAmountEur());
        dto.setInstallment(e.getInstallment());
        dto.setDownpayment(e.getDownpayment());
        dto.setPeriodMonths(e.getPeriodMonths());
        dto.setCustomerFirstName(e.getCustomerFirstName());
        dto.setCustomerLastName(e.getCustomerLastName());
        dto.setCustomerPhone(e.getCustomerPhone());
        dto.setCustomerEmail(e.getCustomerEmail());
        dto.setProductId(e.getProductId());
        dto.setProductName(e.getProductName());
        dto.setStatusHistory(e.getStatusHistory());
        dto.setItemsJson(e.getItemsJson());
        dto.setShippingAddress(e.getShippingAddress());
        dto.setShippingCity(e.getShippingCity());
        dto.setShippingPostalCode(e.getShippingPostalCode());
        dto.setIsToSpeedyOffice(e.getIsToSpeedyOffice());
        dto.setShippingSpeedySiteId(e.getShippingSpeedySiteId());
        dto.setShippingSpeedyOfficeId(e.getShippingSpeedyOfficeId());
        dto.setShippingSpeedySiteName(e.getShippingSpeedySiteName());
        dto.setShippingSpeedyOfficeName(e.getShippingSpeedyOfficeName());

        dto.setWebhookReceivedAt(e.getWebhookReceivedAt());
        dto.setCreatedAt(e.getCreatedAt());
        dto.setUpdatedAt(e.getUpdatedAt());

        if (e.getOrder() != null) {
            dto.setOrderId(e.getOrder().getId());
            dto.setOrderNumber(e.getOrder().getOrderNumber());
        }

        return dto;
    }
}
