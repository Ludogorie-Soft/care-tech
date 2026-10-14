package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogTagRequestDto {

    @NotBlank(message = "Името е задължително")
    @Size(max = 100, message = "Името не може да надвишава 100 символа")
    private String name;

    @Size(max = 100, message = "Slug-ът не може да надвишава 100 символа")
    @Pattern(
            regexp = "^([a-z0-9]([a-z0-9-]*[a-z0-9])?)?$",
            message = "Slug-ът трябва да съдържа само малки латински букви, цифри и тирета"
    )
    private String slug;
}
