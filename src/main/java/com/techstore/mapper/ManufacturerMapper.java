package com.techstore.mapper;

import com.techstore.dto.response.ManufacturerResponseDto;
import com.techstore.entity.Manufacturer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ManufacturerMapper {

    @Mapping(target = "markupPercentage", ignore = true)
    ManufacturerResponseDto toResponseDto(Manufacturer manufacturer);
}
