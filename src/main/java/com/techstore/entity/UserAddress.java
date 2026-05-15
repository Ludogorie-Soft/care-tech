package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_addresses")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String label;

    @Column(length = 20, nullable = false)
    private String type = "address"; // "address" | "speedy_office"

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "city_id", length = 50)
    private String cityId;

    @Column(name = "office_id", length = 50)
    private String officeId;
}
