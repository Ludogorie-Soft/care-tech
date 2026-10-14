package com.techstore.entity;

import com.techstore.enums.BlogPostStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, exclude = {"category", "tags"})
public class BlogPost extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlogPostStatus status = BlogPostStatus.DRAFT;

    @Column(nullable = false)
    private boolean featured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BlogCategory category;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "blog_post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @BatchSize(size = 50)
    private Set<BlogTag> tags = new HashSet<>();

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public boolean isPublished() {
        return BlogPostStatus.PUBLISHED.equals(this.status);
    }
}
