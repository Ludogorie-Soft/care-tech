package com.techstore.dto.tbi;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Returned to the frontend after a successful RegisterApplication call.
 * The frontend opens `url` in the TBI iframe popup.
 */
@Data
@AllArgsConstructor
public class TbiRegisterResponseDto {

    /** Our internal leasing application ID */
    private Long applicationId;

    /** The TBI-provided iframe URL the customer must be directed to */
    private String url;
}
