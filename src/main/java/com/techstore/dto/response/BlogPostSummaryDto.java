package com.techstore.dto.response;

import com.techstore.entity.BlogPost;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class BlogPostSummaryDto {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String coverImageUrl;
    private String status;
    private boolean featured;
    private String categorySlug;
    private String categoryName;
    private List<BlogTagResponseDto> tags;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BlogPostSummaryDto from(BlogPost post) {
        BlogPostSummaryDto dto = new BlogPostSummaryDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setCoverImageUrl(post.getCoverImageUrl());
        dto.setStatus(post.getStatus() != null ? post.getStatus().name() : "DRAFT");
        dto.setFeatured(post.isFeatured());
        dto.setPublishedAt(post.getPublishedAt());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        if (post.getCategory() != null) {
            dto.setCategorySlug(post.getCategory().getSlug());
            dto.setCategoryName(post.getCategory().getName());
        }

        if (post.getTags() != null) {
            dto.setTags(post.getTags().stream()
                    .map(tag -> BlogTagResponseDto.from(tag, 0L))
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toList());
        } else {
            dto.setTags(Collections.emptyList());
        }

        return dto;
    }
}
