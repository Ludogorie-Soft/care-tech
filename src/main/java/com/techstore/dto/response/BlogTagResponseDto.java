package com.techstore.dto.response;

import com.techstore.entity.BlogTag;
import lombok.Data;

@Data
public class BlogTagResponseDto {

    private Long id;
    private String name;
    private String slug;
    private long postCount;

    public static BlogTagResponseDto from(BlogTag tag, long postCount) {
        BlogTagResponseDto dto = new BlogTagResponseDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setSlug(tag.getSlug());
        dto.setPostCount(postCount);
        return dto;
    }
}
