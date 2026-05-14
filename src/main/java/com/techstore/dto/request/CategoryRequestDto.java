package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequestDto {
    private Long id;
    private Long parent;

    @NotBlank(message = "Category name (BG) is required")
    @Size(max = 255, message = "Category name (BG) must not exceed 255 characters")
    private String nameBg;

    @Size(max = 255, message = "Category name (EN) must not exceed 255 characters")
    private String nameEn;

    private Boolean show;

    @Min(value = 0, message = "Category order cannot be negative")
    private Integer order;

    @NotBlank(message = "Category slug is required")
    @Size(max = 255, message = "Category slug must not exceed 255 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Category slug can only contain lowercase letters, numbers, and hyphens")
    private String slug;
}
