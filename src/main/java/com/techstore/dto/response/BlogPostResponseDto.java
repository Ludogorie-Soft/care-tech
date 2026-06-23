package com.techstore.dto.response;

import com.techstore.entity.BlogPost;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class BlogPostResponseDto {

    private Long id;
    private String title;
    private String slug;
    private String excerpt;
    private String content;
    private String coverImageUrl;
    private String status;
    private boolean featured;
    private BlogCategoryResponseDto category;
    private List<BlogTagResponseDto> tags;
    private String metaTitle;
    private String metaDescription;
    private long viewCount;
    private int readTimeMinutes;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    public static BlogPostResponseDto from(BlogPost post) {
        BlogPostResponseDto dto = new BlogPostResponseDto();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setSlug(post.getSlug());
        dto.setExcerpt(post.getExcerpt());
        dto.setContent(post.getContent());
        dto.setCoverImageUrl(post.getCoverImageUrl());
        dto.setStatus(post.getStatus() != null ? post.getStatus().name() : "DRAFT");
        dto.setFeatured(post.isFeatured());
        dto.setMetaTitle(post.getMetaTitle());
        dto.setMetaDescription(post.getMetaDescription());
        dto.setViewCount(post.getViewCount());
        dto.setReadTimeMinutes(computeReadTime(post.getContent()));
        dto.setPublishedAt(post.getPublishedAt());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setUpdatedAt(post.getUpdatedAt());

        if (post.getCategory() != null) {
            dto.setCategory(BlogCategoryResponseDto.from(post.getCategory(), false));
        }

        if (post.getTags() != null) {
            dto.setTags(post.getTags().stream()
                    .map(tag -> BlogTagResponseDto.from(tag, 0L))
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toList());
        }

        return dto;
    }

    private static int computeReadTime(String content) {
        if (content == null || content.isBlank()) return 1;
        String text = HTML_TAG.matcher(content).replaceAll(" ").trim();
        long words = java.util.Arrays.stream(text.split("\\s+"))
                .filter(w -> !w.isBlank()).count();
        return (int) Math.max(1, Math.ceil(words / 200.0));
    }
}
