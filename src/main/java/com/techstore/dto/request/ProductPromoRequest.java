package com.techstore.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductPromoRequest {
    private Long id;
    private BigDecimal discount;
}
