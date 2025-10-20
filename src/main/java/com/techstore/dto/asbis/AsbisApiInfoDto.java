package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisApiInfoDto {
    private Boolean enabled;
    private String baseUrl;
    private Boolean connected;
    private Integer cacheTimeoutMinutes;
    private LocalDateTime lastCacheRefresh;
    private Integer availableProducts;
    private Integer availableCategories;
    private Integer availableManufacturers;
}
