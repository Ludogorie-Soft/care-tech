package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"parent", "children", "products", "specificationTemplates"})
public class Category extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, nullable = false, length = 200)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CategorySpecificationTemplate> specificationTemplates = new ArrayList<>();

    public List<CategorySpecificationTemplate> getRequiredSpecifications() {
        return specificationTemplates.stream()
                .filter(CategorySpecificationTemplate::getRequired)
                .sorted(Comparator.comparing(CategorySpecificationTemplate::getSortOrder))
                .collect(Collectors.toList());
    }

    public List<CategorySpecificationTemplate> getFilterableSpecifications() {
        return specificationTemplates.stream()
                .filter(CategorySpecificationTemplate::getFilterable)
                .sorted(Comparator.comparing(CategorySpecificationTemplate::getSortOrder))
                .collect(Collectors.toList());
    }

    public boolean isParentCategory() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public String getFullPath() {
        if (parent == null) {
            return name;
        }
        return parent.getFullPath() + " / " + name;
    }
}