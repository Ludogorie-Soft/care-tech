package com.techstore.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequestDto {

    @NotNull
    private Long productId;

    @NotNull
    @Min(1) @Max(5)
    private Short rating;

    private String comment;
}
