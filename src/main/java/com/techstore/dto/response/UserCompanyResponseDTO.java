package com.techstore.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserCompanyResponseDTO {
    private Long id;
    private String companyName;
    private String eik;
    private String vatNumber;
    private String mol;
    private String seat;
    private String managementAddress;
    private String city;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
