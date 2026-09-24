package com.techstore.dto.response;

import java.util.List;

/**
 * One row of a product's specification table as shoppers see it: parameters that mean the same thing
 * are merged under one name, identical values appear once, and logistics fields are left out.
 */
public record DisplaySpecificationDto(String name, List<String> values) {
}
