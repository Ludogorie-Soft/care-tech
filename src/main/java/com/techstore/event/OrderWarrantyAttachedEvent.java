package com.techstore.event;

import com.techstore.entity.Order;

/**
 * The admin attached a warranty card to an order and chose to send it to the customer.
 *
 * @param registered whether the order belongs to an account; read in the transaction, because the
 *                   email is built after it, from a detached order
 */
public record OrderWarrantyAttachedEvent(Order order, boolean registered) {
}
