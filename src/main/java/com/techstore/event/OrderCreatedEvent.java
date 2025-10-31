package com.techstore.event;

import com.techstore.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a new order is created
 */
@Getter
public class OrderCreatedEvent extends ApplicationEvent {

    private final Order order;

    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}