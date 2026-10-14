package com.techstore.dto.tbi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Webhook payload POSTed by TBI to our statusURL when an application changes state.
 *
 * Standard loan example:
 * {
 *   "CreditApplicationId": "APP-ID-FROM-BANKLY",
 *   "StatusUrl": "https://api.caretech.bg/api/tbi/webhook",
 *   "OrderId": "123456",
 *   "Status": "3",
 *   "Message": "Approved",
 *   "ResellerCode": "BIVD"
 * }
 *
 * BNPL example:
 * {
 *   "CreditApplicationId": "BNPL-APP-ID",
 *   "StatusUrl": "...",
 *   "OrderId": "123456",
 *   "Status": 1,
 *   "Message": "in_progress",
 *   "ResellerCode": "BIVD",
 *   "System": "FTOS",
 *   "ContractNumber": null
 * }
 *
 * NOTE: "Status" can be a String or Integer depending on product type.
 * We rely on "Message" as the canonical human-readable status value.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbiStatusWebhookDto {

    @JsonProperty("CreditApplicationId")
    private String creditApplicationId;

    @JsonProperty("StatusUrl")
    private String statusUrl;

    /** Matches the orderid we sent in RegisterApplication */
    @JsonProperty("OrderId")
    private String orderId;

    /** Numeric status code — use getMessage() for the readable status */
    @JsonProperty("Status")
    private Object status;

    /** Human-readable status: "Approved", "Rejected", "ContractSigned", etc. */
    @JsonProperty("Message")
    private String message;

    @JsonProperty("ResellerCode")
    private String resellerCode;

    /** Present only for BNPL applications */
    @JsonProperty("System")
    private String system;

    @JsonProperty("ContractNumber")
    private String contractNumber;

    public boolean isBnpl() {
        return "FTOS".equalsIgnoreCase(system);
    }

    /** Returns the canonical status string we store in the DB */
    public String resolvedStatus() {
        return message != null ? message : (status != null ? status.toString() : "Unknown");
    }
}
