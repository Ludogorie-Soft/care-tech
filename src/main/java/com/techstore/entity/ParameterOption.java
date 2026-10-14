package com.techstore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parameter_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParameterOption extends BaseEntity {

    @Column(name = "external_id")
    private Long externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_id", nullable = false)
    private Parameter parameter;

    @Column(name = "name_bg")
    private String nameBg;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "sort_order")
    private Integer order;

    @OneToMany(mappedBy = "parameterOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductParameter> productParameters = new HashSet<>();
}