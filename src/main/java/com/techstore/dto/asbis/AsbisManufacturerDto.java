package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisManufacturerDto {
    private String asbisId;
    private String asbisCode;
    private String name;
    private Integer productCount;
    private Boolean synced;
}
