package com.techstore.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "blog_categories")
@BatchSize(size = 50)
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, exclude = {"parent", "children"})
public class BlogCategory extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BlogCategory parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC, name ASC")
    private List<BlogCategory> children = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
