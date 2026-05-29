package com.techstore.dto.tbi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregated statistics shown on the admin leasing dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeasingApplicationStatisticsDto {

    private long total;
    private long draft;
    private long inProgress;
    private long approved;
    private long contractSigned;
    private long paid;
    private long rejected;
    private long canceled;

    /** Sum of amountEur for all ContractSigned + Paid applications */
    private BigDecimal totalSignedAmountEur;

    /** Sum of amountEur for all Approved applications (not yet signed) */
    private BigDecimal totalApprovedAmountEur;
}
