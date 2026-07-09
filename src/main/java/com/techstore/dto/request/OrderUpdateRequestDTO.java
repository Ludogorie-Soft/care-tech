package com.techstore.dto.request;

import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin-only DTO for full order editing.
 * Patch semantics: null fields are ignored (not overwritten).
 */
@Data
public class OrderUpdateRequestDTO {

    // ── Status ────────────────────────────────────────────────────────────────
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    @Size(max = 100)
    private String trackingNumber;

    @Size(max = 2000)
    private String adminNotes;

    // ── Customer info ─────────────────────────────────────────────────────────
    @Size(max = 100)
    private String customerFirstName;

    @Size(max = 100)
    private String customerLastName;

    @Email
    @Size(max = 255)
    private String customerEmail;

    @Size(max = 50)
    private String customerPhone;

    @Size(max = 255)
    private String customerCompany;

    @Size(max = 50)
    private String customerVatNumber;

    // ── Shipping address ──────────────────────────────────────────────────────
    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 100)
    private String shippingCity;

    @Size(max = 20)
    private String shippingPostalCode;

    @Size(max = 100)
    private String shippingCountry;

    private BigDecimal shippingCost;

    // ── Invoice ───────────────────────────────────────────────────────────────
    @Size(max = 100)
    private String invoiceNumber;

    private LocalDateTime invoiceDate;

    // ── Order items ───────────────────────────────────────────────────────────
    /** null = do not touch items. quantity = 0 removes the item. */
    @Valid
    private List<OrderItemUpdateDTO> items;
}
