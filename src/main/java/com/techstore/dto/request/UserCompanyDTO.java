package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCompanyDTO {

    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    private String companyName;

    @Size(max = 50, message = "EIK must not exceed 50 characters")
    private String eik;

    @Size(max = 50, message = "VAT number must not exceed 50 characters")
    private String vatNumber;

    @Size(max = 100, message = "MOL must not exceed 100 characters")
    private String mol;

    @Size(max = 500, message = "Seat must not exceed 500 characters")
    private String seat;

    @Size(max = 500, message = "Management address must not exceed 500 characters")
    private String managementAddress;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    private Boolean isDefault = false;
}
