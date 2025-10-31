package com.techstore.event;

import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when order status is changed
 */
@Getter
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Order order;
    private final OrderStatus previousStatus;
    private final OrderStatus newStatus;

    public OrderStatusChangedEvent(Object source, Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        super(source);
        this.order = order;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }
}