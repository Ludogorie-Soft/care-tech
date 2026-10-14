package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.request.OrderUpdateRequestDTO;
import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentStatus;
import com.techstore.event.OrderStatusChangedEvent;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — бележка към клиента и имейл при смяна на статус")
class OrderServiceStatusNotificationTest {

    @Mock OrderRepository           orderRepository;
    @Mock ProductRepository         productRepository;
    @Mock UserRepository            userRepository;
    @Mock CartItemRepository        cartItemRepository;
    @Mock UserService               userService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock FileUploadService         fileUploadService;
    @Mock S3Service                 s3Service;
    @Mock ShippingConfig            shippingConfig;

    @InjectMocks OrderService service;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-2026-00001");
        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setAdminNotes("Стара бележка");
        order.setTrackingNumber("111");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ── Quick status change (UpdateOrderStatusModal) ────────────────────────────

    @Test
    void notifyCustomerFalseSavesWithoutEmail() {
        service.updateOrderStatus(1L, statusUpdate(OrderStatus.SHIPPED, "222", "Тест", false));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingNumber()).isEqualTo("222");
        assertThat(order.getAdminNotes()).isEqualTo("Тест");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void notifyCustomerNullStillEmailsForOlderClients() {
        service.updateOrderStatus(1L, statusUpdate(OrderStatus.SHIPPED, null, null, null));

        OrderStatusChangedEvent event = publishedEvent();
        assertThat(event.getPreviousStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(event.getNewStatus()).isEqualTo(OrderStatus.SHIPPED);
        // Null leaves the stored note and tracking number alone, and the email shows that note
        assertThat(order.getAdminNotes()).isEqualTo("Стара бележка");
        assertThat(order.getTrackingNumber()).isEqualTo("111");
        assertThat(event.getCustomerNote()).isEqualTo("Стара бележка");
    }

    @Test
    void blankNoteAndTrackingNumberClearTheStoredValues() {
        service.updateOrderStatus(1L, statusUpdate(OrderStatus.DELIVERED, "  ", "   ", true));

        assertThat(order.getAdminNotes()).isNull();
        assertThat(order.getTrackingNumber()).isNull();
        assertThat(publishedEvent().getCustomerNote()).isNull();
    }

    @Test
    void noteIsTrimmedAndSentWithTheEmail() {
        service.updateOrderStatus(1L, statusUpdate(OrderStatus.CANCELLED, null, "  Няма наличност.\n ", true));

        assertThat(order.getAdminNotes()).isEqualTo("Няма наличност.");
        assertThat(publishedEvent().getCustomerNote()).isEqualTo("Няма наличност.");
    }

    @Test
    void savingTheSameStatusStillEmailsWhenAsked() {
        service.updateOrderStatus(1L, statusUpdate(OrderStatus.PROCESSING, null, "Ново", true));

        OrderStatusChangedEvent event = publishedEvent();
        assertThat(event.getPreviousStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(event.getNewStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(event.getCustomerNote()).isEqualTo("Ново");
    }

    // ── Full edit (EditOrderModal) ──────────────────────────────────────────────

    @Test
    void editWithStatusChangeEmailsTheSavedNote() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setStatus(OrderStatus.SHIPPED);
        request.setAdminNotes(" Изпратено днес ");

        service.updateOrder(1L, request);

        assertThat(order.getAdminNotes()).isEqualTo("Изпратено днес");
        assertThat(publishedEvent().getCustomerNote()).isEqualTo("Изпратено днес");
    }

    @Test
    void editWithNotifyCustomerFalseSendsNothing() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setStatus(OrderStatus.SHIPPED);
        request.setNotifyCustomer(false);

        service.updateOrder(1L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void editCanClearTheNote() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setAdminNotes("");

        service.updateOrder(1L, request);

        assertThat(order.getAdminNotes()).isNull();
        verifyNoInteractions(eventPublisher);
    }

    // ── Customer cancel ─────────────────────────────────────────────────────────

    @Test
    void customerCancelEmailsOnlyTheReasonAndKeepsABulgarianAuditLine() {
        service.cancelOrder(1L, " Размислих ");

        assertThat(publishedEvent().getCustomerNote()).isEqualTo("Размислих");
        assertThat(order.getAdminNotes())
                .startsWith("Стара бележка\nПричина за отмяна: Размислих (")
                .matches("(?s).*\\(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}\\)$")
                .doesNotContain("Cancellation reason");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void customerCancelWithoutReason() {
        order.setAdminNotes(null);

        service.cancelOrder(1L, "");

        assertThat(publishedEvent().getCustomerNote()).isNull();
        assertThat(order.getAdminNotes()).startsWith("Отменена (").doesNotContain("null");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private OrderStatusChangedEvent publishedEvent() {
        ArgumentCaptor<OrderStatusChangedEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static OrderStatusUpdateDTO statusUpdate(OrderStatus status, String trackingNumber, String adminNotes,
                                                     Boolean notifyCustomer) {
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO();
        dto.setStatus(status);
        dto.setTrackingNumber(trackingNumber);
        dto.setAdminNotes(adminNotes);
        dto.setNotifyCustomer(notifyCustomer);
        return dto;
    }
}
