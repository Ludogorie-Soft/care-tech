package com.techstore.entity;

import com.techstore.enums.Platform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "manufacturers")
@Indexed
@Getter
@Setter
public class Manufacturer extends BaseEntity {

    @Column(name = "is_promo_active", nullable = false)
    private Boolean isPromoActive = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private Platform platform;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "asbis_id")
    private String asbisId;

    @Column(name = "asbis_code")
    private String asbisCode;

    @FullTextField
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "information_name")
    private String informationName;

    @Column(name = "information_email")
    private String informationEmail;

    @Column(name = "information_address", columnDefinition = "TEXT")
    private String informationAddress;

    @Column(name = "eu_representative_name")
    private String euRepresentativeName;

    @Column(name = "eu_representative_email")
    private String euRepresentativeEmail;

    @Column(name = "eu_representative_address", columnDefinition = "TEXT")
    private String euRepresentativeAddress;

    @OneToMany(mappedBy = "manufacturer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();
}