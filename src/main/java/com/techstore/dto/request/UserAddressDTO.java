package com.techstore.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserAddressDTO {

    @Size(max = 100, message = "Label must not exceed 100 characters")
    private String label;

    @NotBlank(message = "Address type is required")
    @Pattern(regexp = "address|speedy_office", message = "Type must be 'address' or 'speedy_office'")
    private String type;

    @Size(max = 500, message = "Address line must not exceed 500 characters")
    private String addressLine;

    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    private Boolean isDefault = false;

    @Size(max = 50, message = "City ID must not exceed 50 characters")
    private String cityId;

    @Size(max = 50, message = "Office ID must not exceed 50 characters")
    private String officeId;
}
