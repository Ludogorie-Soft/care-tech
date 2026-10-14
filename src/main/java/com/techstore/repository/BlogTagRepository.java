package com.techstore.repository;

import com.techstore.entity.BlogTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlogTagRepository extends JpaRepository<BlogTag, Long> {

    Optional<BlogTag> findBySlug(String slug);

    Optional<BlogTag> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByNameIgnoreCase(String name);

    List<BlogTag> findAllByOrderByNameAsc();

    @Query(value = "SELECT tag_id, COUNT(post_id) FROM blog_post_tags GROUP BY tag_id", nativeQuery = true)
    List<Object[]> countPostsPerTag();

    @Query("SELECT COUNT(p) FROM BlogPost p JOIN p.tags t WHERE t.id = :id")
    long countPostsByTagId(@Param("id") Long id);
}
