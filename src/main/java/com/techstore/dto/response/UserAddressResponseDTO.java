package com.techstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserAddressResponseDTO {
    private Long id;
    private String label;
    private String type;
    private String addressLine;
    private String city;
    private String postalCode;
    private Boolean isDefault;
    private String cityId;
    private String officeId;
    private LocalDateTime createdAt;
}
