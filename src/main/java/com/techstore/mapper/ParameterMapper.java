package com.techstore.mapper;

import com.techstore.dto.response.ParameterOptionResponseDto;
import com.techstore.dto.response.ParameterResponseDto;
import com.techstore.entity.Category;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ParameterMapper {

    @Mapping(target = "name", expression = "java(getLocalizedName(parameter, language))")
    @Mapping(target = "categoryId", expression = "java(getPrimaryCategoryId(parameter))")
    @Mapping(target = "categoryName", expression = "java(getPrimaryCategoryName(parameter, language))")
    @Mapping(target = "categoryIds", expression = "java(getAllCategoryIds(parameter))")
    @Mapping(target = "options", expression = "java(mapOptions(parameter, language))")
    ParameterResponseDto toResponseDto(Parameter parameter, @Context String language);

    @Mapping(target = "name", expression = "java(getLocalizedOptionName(option, language))")
    @Mapping(target = "parameterId", source = "parameter.id")
    @Mapping(target = "parameterName", expression = "java(getParameterName(option, language))")
    ParameterOptionResponseDto toOptionResponseDto(ParameterOption option, @Context String language);

    default String getLocalizedName(Parameter parameter, String language) {
        return "bg".equals(language) ? parameter.getNameBg() : parameter.getNameEn();
    }

    default String getLocalizedOptionName(ParameterOption option, String language) {
        return "bg".equals(language) ? option.getNameBg() : option.getNameEn();
    }

    /**
     * Get primary category ID (first category from the set)
     * For backward compatibility with existing API responses
     */
    default Long getPrimaryCategoryId(Parameter parameter) {
        if (parameter.getCategories() == null || parameter.getCategories().isEmpty()) {
            return null;
        }
        return parameter.getCategories().stream()
                .findFirst()
                .map(Category::getId)
                .orElse(null);
    }

    /**
     * Get primary category name (first category from the set)
     * For backward compatibility with existing API responses
     */
    default String getPrimaryCategoryName(Parameter parameter, String language) {
        if (parameter.getCategories() == null || parameter.getCategories().isEmpty()) {
            return null;
        }

        Category primaryCategory = parameter.getCategories().stream()
                .findFirst()
                .orElse(null);

        if (primaryCategory == null) {
            return null;
        }

        return "bg".equals(language) ? primaryCategory.getNameBg() : primaryCategory.getNameEn();
    }

    /**
     * Get all category IDs that this parameter belongs to
     * This is NEW functionality for Many-to-Many support
     */
    default List<Long> getAllCategoryIds(Parameter parameter) {
        if (parameter.getCategories() == null || parameter.getCategories().isEmpty()) {
            return List.of();
        }

        return parameter.getCategories().stream()
                .map(Category::getId)
                .sorted()
                .collect(Collectors.toList());
    }

    default String getParameterName(ParameterOption option, String language) {
        if (option.getParameter() == null) return null;
        return "bg".equals(language) ? option.getParameter().getNameBg() : option.getParameter().getNameEn();
    }

    default List<ParameterOptionResponseDto> mapOptions(Parameter parameter, String language) {
        if (parameter.getOptions() == null) return List.of();
        return parameter.getOptions().stream()
                .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
                .map(option -> toOptionResponseDto(option, language))
                .collect(Collectors.toList());
    }
}