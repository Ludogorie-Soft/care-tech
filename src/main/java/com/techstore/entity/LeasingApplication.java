package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leasing_applications")
@Data
@EqualsAndHashCode(callSuper = false)
public class LeasingApplication extends BaseEntity {

    // ── Link to our orders table ─────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    // ── TBI identifiers ──────────────────────────────────────────────────────

    /** order_id returned by TBI RegisterApplication */
    @Column(name = "tbi_order_id")
    private Integer tbiOrderId;

    /** token returned by TBI RegisterApplication — needed to poll status */
    @Column(name = "tbi_token", length = 200)
    private String tbiToken;

    /** CreditApplicationId sent by TBI via the status webhook */
    @Column(name = "tbi_application_id", length = 200)
    private String tbiApplicationId;

    /** The orderid we sent to TBI ("0" for express-buy, or our ORD-XXXX once assigned) */
    @Column(name = "store_order_id", length = 100)
    private String storeOrderId;

    // ── Status ───────────────────────────────────────────────────────────────

    /**
     * Current TBI status string.
     * Standard: Draft | MP Sent | InProgress | InProgressAssessment | Approved |
     *           KYC | ContractSigning | ContractSigned | Paid | Rejected | Canceled
     * BNPL    : in_progress | approved & signed | rejected | canceled | expired
     */
    @Column(name = "status", length = 100, nullable = false)
    private String status = "Draft";

    // ── Financial details ────────────────────────────────────────────────────

    @Column(name = "amount_eur", precision = 10, scale = 2)
    private BigDecimal amountEur;

    @Column(name = "installment", precision = 10, scale = 2)
    private BigDecimal installment;

    @Column(name = "downpayment", precision = 10, scale = 2)
    private BigDecimal downpayment;

    @Column(name = "period_months")
    private Integer periodMonths;

    // ── Customer info (denormalised) ─────────────────────────────────────────

    @Column(name = "customer_first_name", length = 100)
    private String customerFirstName;

    @Column(name = "customer_last_name", length = 100)
    private String customerLastName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "customer_email", length = 200)
    private String customerEmail;

    // ── Product info (denormalised) ──────────────────────────────────────────

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", length = 500)
    private String productName;

    // ── Shipping address ─────────────────────────────────────────────────────

    @Column(name = "shipping_address", length = 200)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_postal_code", length = 20)
    private String shippingPostalCode;

    @Column(name = "is_to_speedy_office")
    private Boolean isToSpeedyOffice = false;

    @Column(name = "shipping_speedy_site_id")
    private Long shippingSpeedySiteId;

    @Column(name = "shipping_speedy_office_id")
    private Long shippingSpeedyOfficeId;

    @Column(name = "shipping_speedy_site_name", length = 200)
    private String shippingSpeedySiteName;

    @Column(name = "shipping_speedy_office_name", length = 200)
    private String shippingSpeedyOfficeName;

    // ── JSON audit payloads ───────────────────────────────────────────────────

    /** Serialised list of TbiItemDto objects sent to TBI */
    @Column(name = "items_json", columnDefinition = "TEXT")
    private String itemsJson;

    /**
     * JSON array of status-change events.
     * Format: [{"datetime":"2024-01-01T10:00:00","status":"Draft"}, ...]
     */
    @Column(name = "status_history", columnDefinition = "TEXT")
    private String statusHistory;

    /** Raw JSON response from TBI RegisterApplication */
    @Column(name = "tbi_raw_response", columnDefinition = "TEXT")
    private String tbiRawResponse;

    /**
     * Serialised OrderCreateRequestDTO — used to create the order lazily
     * when the ContractSigned webhook arrives. Null after the order is created.
     */
    @Column(name = "pending_order_json", columnDefinition = "TEXT")
    private String pendingOrderJson;

    // ── Timestamps ────────────────────────────────────────────────────────────

    @Column(name = "webhook_received_at")
    private LocalDateTime webhookReceivedAt;
}
