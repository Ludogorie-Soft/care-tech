package com.techstore.dto.tbi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response from TBI POST /api/RegisterApplication.
 *
 * Success example:
 * {
 *   "error": 0,
 *   "order_id": 22,
 *   "token": "bdf54849927eed3d4b785fe1e812582a17ad",
 *   "url": "https://beta.tbibank.support/application/?order_id=22&token=..."
 * }
 *
 * Error example:
 * { "error": 205, "message": "Access denied /api/info205" }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbiRegisterApplicationResponseDto {

    private Integer error;

    @JsonProperty("order_id")
    private Integer orderId;

    private String token;

    /** The URL to load in the iframe for the customer to complete the application */
    private String url;

    private String message;

    public boolean isSuccess() {
        return error != null && error == 0 && url != null;
    }
}
