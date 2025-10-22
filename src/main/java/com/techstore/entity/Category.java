package com.techstore.entity;

import com.techstore.enums.Platform;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Data
@EqualsAndHashCode(callSuper = false, exclude = {"parent", "children", "products"})
public class Category extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private Platform platform;

    @Column(name = "tekra_id")
    private String tekraId;

    @Column(name = "tekra_slug")
    private String tekraSlug;

    @Column(name = "asbis_id")
    private String asbisId;

    @Column(name = "asbis_code")
    private String asbisCode;

    @Column(name = "external_id", unique = true)
    private Long externalId;

    @Column(name = "category_path", length = 500)
    private String categoryPath;

    @FullTextField
    @Column(name = "name_en")
    private String nameEn;

    @FullTextField
    @Column(name = "name_bg")
    private String nameBg;

    @Column(length = 200)
    private String slug;

    @Column(name = "show_flag", nullable = false)
    private Boolean show = true;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    public boolean isParentCategory() {
        return parent == null;
    }

    public String generateCategoryPath() {
        List<String> pathParts = new ArrayList<>();
        Category current = this;

        while (current != null) {
            if (current.getTekraSlug() != null && !current.getTekraSlug().trim().isEmpty()) {
                pathParts.add(0, current.getTekraSlug());
            }
            else if (current.getSlug() != null) {
                String baseSlug = extractBaseSlug(current.getSlug(), current.getParent());
                pathParts.add(0, baseSlug);
            }
            current = current.getParent();
        }

        return pathParts.isEmpty() ? null : String.join("/", pathParts);
    }

    private String extractBaseSlug(String fullSlug, Category parent) {
        if (parent == null || parent.getSlug() == null) {
            return fullSlug;
        }

        String parentSlug = parent.getSlug();
        if (fullSlug.startsWith(parentSlug + "-")) {
            return fullSlug.substring(parentSlug.length() + 1);
        }

        return fullSlug;
    }
}