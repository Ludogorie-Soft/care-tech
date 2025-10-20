package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisProductDto {
    private String asbisId;
    private String asbisCode;
    private String asbisPartNumber;

    private String name;
    private String description;
    private String model;
    private String sku;
    private String barcode;

    private BigDecimal price;
    private Integer stock;
    private BigDecimal weight;

    private String categoryId;
    private String categoryName;
    private String vendor;

    private String primaryImage;
    private List<String> additionalImages;
    private Map<String, String> specifications;
}
