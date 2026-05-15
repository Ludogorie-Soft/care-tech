package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "user_companies")
@Data
@EqualsAndHashCode(callSuper = false)
public class UserCompany extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", length = 200, nullable = false)
    private String companyName;

    @Column(length = 50)
    private String eik;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(length = 100)
    private String mol;

    @Column(columnDefinition = "TEXT")
    private String seat;

    @Column(name = "management_address", columnDefinition = "TEXT")
    private String managementAddress;

    @Column(length = 100)
    private String city;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
}
