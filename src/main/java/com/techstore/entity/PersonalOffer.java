package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.techstore.enums.PaymentMethod;
import com.techstore.enums.ShippingMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "personal_offers")
@Data
@EqualsAndHashCode(callSuper = false)
public class PersonalOffer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "offer_items", columnDefinition = "jsonb")
    private List<OfferItem> offerItems;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status = OfferStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_details", columnDefinition = "jsonb")
    private ShippingDetails shippingDetails;

    public enum OfferStatus {
        PENDING, READ, ACCEPTED, REJECTED, CONVERTED
    }

    @Data
    public static class ShippingDetails {
        private String customerPhone;
        private String shippingAddress;
        private String shippingCity;
        private String shippingPostalCode;
        private String shippingCountry;
        private Boolean isToSpeedyOffice;
        private Long shippingSpeedySiteId;
        private Long shippingSpeedyOfficeId;
        private String shippingSpeedySiteName;
        private String shippingSpeedyOfficeName;
        private PaymentMethod paymentMethod;
        private ShippingMethod shippingMethod;
        private String customerCompany;
        private String customerVatNumber;
        private Boolean customerVatRegistered;
        private String customerNotes;
    }

    @Data
    public static class OfferItem {
        private Long productId;
        private String productName;
        private String imageUrl;
        private BigDecimal originalPrice;
        private BigDecimal offerPrice;
        private BigDecimal discountPercent;
        private Integer quantity;
    }
}
