package com.techstore.listener;

import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import com.techstore.event.OrderCreatedEvent;
import com.techstore.event.OrderStatusChangedEvent;
import com.techstore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for order-related events
 * Handles sending email notifications when orders are created or updated
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final EmailService emailService;

    /**
     * Handle order created event
     * Sends confirmation email to customer
     * Sends email to admin
     */
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent for order: {}", event.getOrder().getOrderNumber());

        try {
            Order order = event.getOrder();
            emailService.sendOrderConfirmationEmail(order);
            emailService.sendNewOrderNotificationToAdmin(order);
        } catch (Exception e) {
            log.error("Error handling order created event", e);
        }
    }

    /**
     * Handle order status changed event
     * Sends appropriate email based on the new status
     */
    @EventListener
    @Async
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Processing OrderStatusChangedEvent for order: {} (from {} to {})",
                event.getOrder().getOrderNumber(),
                event.getPreviousStatus(),
                event.getNewStatus());

        try {
            Order order = event.getOrder();
            OrderStatus newStatus = event.getNewStatus();

            // Send specific email based on status
            switch (newStatus) {
                case SHIPPED -> emailService.sendOrderShippedEmail(order);
                case DELIVERED -> emailService.sendOrderDeliveredEmail(order);
                case CANCELLED -> emailService.sendOrderCancelledEmail(order);
                default -> emailService.sendOrderStatusUpdateEmail(order, event.getPreviousStatus());
            }
        } catch (Exception e) {
            log.error("Error handling order status changed event", e);
        }
    }
}