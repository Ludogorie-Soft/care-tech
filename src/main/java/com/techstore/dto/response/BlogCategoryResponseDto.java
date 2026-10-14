package com.techstore.dto.response;

import com.techstore.entity.BlogCategory;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BlogCategoryResponseDto {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long parentId;
    private String parentName;
    private int sortOrder;
    private List<BlogCategoryResponseDto> children;

    public static BlogCategoryResponseDto from(BlogCategory cat, boolean includeChildren) {
        BlogCategoryResponseDto dto = new BlogCategoryResponseDto();
        dto.setId(cat.getId());
        dto.setName(cat.getName());
        dto.setSlug(cat.getSlug());
        dto.setDescription(cat.getDescription());
        dto.setSortOrder(cat.getSortOrder());

        if (cat.getParent() != null) {
            dto.setParentId(cat.getParent().getId());
            dto.setParentName(cat.getParent().getName());
        }

        if (includeChildren && cat.getChildren() != null) {
            dto.setChildren(
                    cat.getChildren().stream()
                            .map(child -> BlogCategoryResponseDto.from(child, true))
                            .toList()
            );
        } else {
            dto.setChildren(new ArrayList<>());
        }

        return dto;
    }
}
