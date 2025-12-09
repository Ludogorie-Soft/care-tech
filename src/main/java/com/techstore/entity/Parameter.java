package com.techstore.entity;

import com.techstore.enums.Platform;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, exclude = {"categories", "options", "productParameters"})
public class Parameter extends BaseEntity {

    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "name_bg")
    private String nameBg;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "sort_order")
    private Integer order;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private Platform platform;

    @Column(name = "tekra_key")
    private String tekraKey;

    private Boolean isFilter = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "category_parameters",
            joinColumns = @JoinColumn(name = "parameter_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @OneToMany(mappedBy = "parameter", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ParameterOption> options = new HashSet<>();

    @OneToMany(mappedBy = "parameter", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductParameter> productParameters = new HashSet<>();

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Parameter)) return false;
        Parameter that = (Parameter) o;
        return getId() != null && getId().equals(that.getId());
    }
}