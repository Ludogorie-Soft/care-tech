package com.techstore.dto.tbi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

/**
 * Delivery address within the TBI application payload.
 * All fields are optional — TBI will ask the customer to fill in missing ones.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TbiDeliveryAddressDto {

    private String country;
    private String county;
    private String city;
    private String streetname;
    private String streetno;
    private String buildingno;
    private String entranceno;
    private String floorno;
    private String apartmentno;
    private String postalcode;

    public static TbiDeliveryAddressDto empty() {
        return TbiDeliveryAddressDto.builder()
                .country("Bulgaria")
                .county("")
                .city("")
                .streetname("")
                .streetno("")
                .buildingno("")
                .entranceno("")
                .floorno("")
                .apartmentno("")
                .postalcode("")
                .build();
    }
}
