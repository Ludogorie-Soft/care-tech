package com.techstore.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProductParameterCreateDTO {

    @NotNull(message = "Parameter ID is required")
    private Long parameterId;

    @NotNull(message = "Parameter option ID is required")
    private List<Long> parameterOptionId;
}