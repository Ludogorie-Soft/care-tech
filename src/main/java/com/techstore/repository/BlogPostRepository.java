package com.techstore.repository;

import com.techstore.entity.BlogPost;
import com.techstore.enums.BlogPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    Page<BlogPost> findByStatusOrderByPublishedAtDesc(BlogPostStatus status, Pageable pageable);

    Page<BlogPost> findByStatus(BlogPostStatus status, Pageable pageable);

    Page<BlogPost> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<BlogPost> findByStatusAndTitleContainingIgnoreCase(BlogPostStatus status, String title, Pageable pageable);

    @Query("SELECT b FROM BlogPost b WHERE b.category.slug = :slug AND b.status = :status ORDER BY b.publishedAt DESC")
    Page<BlogPost> findPublishedByCategorySlug(@Param("slug") String slug, @Param("status") BlogPostStatus status, Pageable pageable);

    @Query("SELECT DISTINCT b FROM BlogPost b JOIN b.tags t WHERE t.slug = :slug AND b.status = :status ORDER BY b.publishedAt DESC")
    Page<BlogPost> findPublishedByTagSlug(@Param("slug") String slug, @Param("status") BlogPostStatus status, Pageable pageable);

    Page<BlogPost> findByFeaturedTrueAndStatusOrderByPublishedAtDesc(BlogPostStatus status, Pageable pageable);

    Optional<BlogPost> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    List<BlogPost> findByStatusAndPublishedAtLessThanEqual(BlogPostStatus status, LocalDateTime now);

    @Modifying
    @Query("UPDATE BlogPost b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Query("SELECT b.id as id, b.slug as slug, b.updatedAt as updatedAt FROM BlogPost b WHERE b.status = :status ORDER BY b.updatedAt DESC")
    List<SitemapEntry> findPublishedSitemapEntries(@Param("status") BlogPostStatus status);
}
