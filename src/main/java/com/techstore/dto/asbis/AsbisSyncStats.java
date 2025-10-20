package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisSyncStats {
    private Long totalProducts;
    private Long totalCategories;
    private Long totalManufacturers;
    private Long totalParameters;

    private Long asbisProducts;
    private Long asbisCategories;
    private Long asbisManufacturers;
    private Long asbisParameters;

    private Map<String, Object> apiStats;
}