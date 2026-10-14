package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlogPostRequestDto {

    @NotBlank(message = "Заглавието е задължително")
    @Size(max = 255, message = "Заглавието не може да надвишава 255 символа")
    private String title;

    @Size(max = 255, message = "Slug-ът не може да надвишава 255 символа")
    @Pattern(
            regexp = "^([a-z0-9]([a-z0-9-]*[a-z0-9])?)?$",
            message = "Slug-ът трябва да започва и завършва с буква или цифра, и да съдържа само малки латински букви, цифри и тирета"
    )
    private String slug;

    @Size(max = 500, message = "Резюмето не може да надвишава 500 символа")
    private String excerpt;

    @NotBlank(message = "Съдържанието е задължително")
    @Size(max = 200_000, message = "Съдържанието не може да надвишава 200 000 символа")
    private String content;

    @Size(max = 500, message = "URL-ът на корицата не може да надвишава 500 символа")
    @Pattern(regexp = "^(https://.*)?$", message = "URL-ът на корицата трябва да е https")
    private String coverImageUrl;

    @Pattern(
            regexp = "^(DRAFT|PUBLISHED|SCHEDULED)?$",
            message = "Статусът трябва да е DRAFT, PUBLISHED или SCHEDULED"
    )
    private String status = "DRAFT";

    private boolean featured = false;

    private Long categoryId;

    private List<Long> tagIds = new ArrayList<>();

    @Size(max = 255, message = "SEO заглавието не може да надвишава 255 символа")
    private String metaTitle;

    @Size(max = 500, message = "SEO описанието не може да надвишава 500 символа")
    private String metaDescription;

    private LocalDateTime publishedAt;
}
