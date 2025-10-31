package com.techstore.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponseDTO {

    // Revenue statistics
    private BigDecimal dailyRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal yearlyRevenue;
    private BigDecimal totalRevenue;

    // Order counts by status
    private Long totalOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippedOrders;
    private Long deliveredOrders;
    private Long cancelledOrders;

    // Active orders (pending + processing + shipped)
    private Long activeOrders;

    // Customer statistics
    private Long totalCustomers;
    private Long newCustomersToday;
    private Long newCustomersThisMonth;
    private Long newCustomersThisYear;

    // Order statistics
    private Long ordersToday;
    private Long ordersThisMonth;
    private Long ordersThisYear;

    // Average order value
    private BigDecimal averageOrderValue;
    private BigDecimal averageOrderValueThisMonth;

    // Payment statistics
    private Long paidOrders;
    private Long unpaidOrders;
    private BigDecimal unpaidRevenue;

    // Top performing metrics
    private BigDecimal highestOrderValue;
    private BigDecimal lowestOrderValue;
}