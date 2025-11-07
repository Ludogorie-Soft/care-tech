package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductParameterResponseDto {
    private Long parameterId;
    private String parameterNameEn;
    private String parameterNameBg;
    private Set<ParameterOptionResponseDto> options;
}
