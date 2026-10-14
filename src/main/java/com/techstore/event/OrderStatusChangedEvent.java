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

    /**
     * Message shown to the customer in the status email ("Информация", or "Причина за отмяна"
     * when cancelled). Captured when the event is published, because the listener runs async
     * and must not pick up a later edit of the order. Null when there is nothing to show.
     */
    private final String customerNote;

    public OrderStatusChangedEvent(Object source, Order order, OrderStatus previousStatus,
                                   OrderStatus newStatus, String customerNote) {
        super(source);
        this.order = order;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.customerNote = customerNote;
    }
}
