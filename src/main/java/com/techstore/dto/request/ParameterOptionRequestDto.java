package com.techstore.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.techstore.dto.external.NameDto;
import lombok.Data;

import java.util.List;

@Data
public class ParameterOptionRequestDto {
    @JsonProperty("externalId")
    @JsonAlias("id")
    private Long externalId;
    private List<NameDto> name;
    private Integer order;
}
