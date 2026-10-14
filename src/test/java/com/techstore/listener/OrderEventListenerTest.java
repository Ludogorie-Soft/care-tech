package com.techstore.listener;

import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import com.techstore.event.OrderStatusChangedEvent;
import com.techstore.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock EmailService emailService;

    @InjectMocks OrderEventListener listener;

    private final Order order = new Order();

    @Test
    void shippedEmailGetsTheNote() {
        listener.handleOrderStatusChanged(event(OrderStatus.PROCESSING, OrderStatus.SHIPPED, "бележка"));
        verify(emailService).sendOrderShippedEmail(order, "бележка");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void deliveredEmailGetsTheNote() {
        listener.handleOrderStatusChanged(event(OrderStatus.SHIPPED, OrderStatus.DELIVERED, "отзив"));
        verify(emailService).sendOrderDeliveredEmail(order, "отзив");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void cancelledEmailGetsTheNote() {
        listener.handleOrderStatusChanged(event(OrderStatus.PENDING, OrderStatus.CANCELLED, "Няма наличност."));
        verify(emailService).sendOrderCancelledEmail(order, "Няма наличност.");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void otherStatusesGetTheGenericEmailWithTheNote() {
        listener.handleOrderStatusChanged(event(OrderStatus.PENDING, OrderStatus.CONFIRMED, null));
        verify(emailService).sendOrderStatusUpdateEmail(order, OrderStatus.PENDING, null);
        verifyNoMoreInteractions(emailService);
    }

    private OrderStatusChangedEvent event(OrderStatus from, OrderStatus to, String note) {
        order.setOrderNumber("ORD-2026-00001");
        return new OrderStatusChangedEvent(this, order, from, to, note);
    }
}
