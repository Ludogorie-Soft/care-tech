package com.techstore.dto.tbi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * The plain-text JSON object that gets AES-256-CTR encrypted and placed
 * in the "data" field of a RegisterApplication request.
 *
 * Field names are lowercase to match the TBI API exactly.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TbiApplicationDataDto {

    /**
     * Our order ID. Use "0" for the express-buy product-page flow —
     * TBI will send the customer's data back to our statusURL/express callback.
     */
    private String orderid;

    private String firstname;
    private String lastname;
    private String surname;
    private String email;
    private String phone;

    private TbiDeliveryAddressDto deliveryaddress;

    private List<TbiItemDto> items;

    /** Pre-select a specific repayment period in months */
    private Integer period;

    /** Pre-select the promotional 0% scheme if available */
    private Boolean promo;

    /** URL to redirect after successful application (optional) */
    private String successRedirectURL;

    /** URL to redirect after failed application / BNPL rejection (optional) */
    private String failRedirectURL;

    /**
     * Our webhook endpoint — TBI will POST status updates here.
     * Must be a publicly reachable HTTPS URL.
     */
    private String statusURL;

    /** Allow BNPL option for this application (optional, product-level control) */
    private Integer bnpl;
}
