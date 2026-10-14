package com.techstore.enums;

/**
 * What delivering one product on its own to a Speedy office costs. The product page shows it
 * under the price and the Pazaruvaj feed sends it as DeliveryCost — the two have to agree.
 */
public enum DeliveryCharge {
    /** The flat office rate ({@code shipping.cost.default}). */
    FIXED,
    FREE,
    /** By the courier's own tariff — not a fixed amount, so the feed leaves DeliveryCost out. */
    COURIER_TARIFF
}
