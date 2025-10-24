package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParameterOptionResponseDto {
    private Long id;
    private Long externalId;
    private String name;
    private Long parameterId;
    private String parameterName;
    private Integer order;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
