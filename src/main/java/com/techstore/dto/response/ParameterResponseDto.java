package com.techstore.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ParameterResponseDto {
    private Long id;
    private Long externalId;
    private String name;

    private Long categoryId;
    private String categoryName;

    private List<Long> categoryIds;

    private Integer order;
    private String platform;
    private String tekraKey;
    private List<ParameterOptionResponseDto> options;

    private Boolean isFilter;
}