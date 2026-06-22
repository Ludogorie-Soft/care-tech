package com.techstore.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogCategoryRequestDto {

    @NotBlank(message = "Името е задължително")
    @Size(max = 100, message = "Името не може да надвишава 100 символа")
    private String name;

    @Size(max = 100, message = "Slug-ът не може да надвишава 100 символа")
    @Pattern(
            regexp = "^([a-z0-9]([a-z0-9-]*[a-z0-9])?)?$",
            message = "Slug-ът трябва да съдържа само малки латински букви, цифри и тирета"
    )
    private String slug;

    @Size(max = 2000, message = "Описанието не може да надвишава 2000 символа")
    private String description;

    private Long parentId;

    @Min(value = 0, message = "Наредбата трябва да е неотрицателно число")
    private int sortOrder = 0;
}
