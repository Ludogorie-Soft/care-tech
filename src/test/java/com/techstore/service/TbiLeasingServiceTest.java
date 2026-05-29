package com.techstore.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.config.TbiConfig;
import com.techstore.dto.tbi.TbiStatusWebhookDto;
import com.techstore.entity.LeasingApplication;
import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import com.techstore.repository.LeasingApplicationRepository;
import com.techstore.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TbiLeasingService — webhook обработка")
class TbiLeasingServiceTest {

    @Mock TbiConfig                    tbiConfig;
    @Mock LeasingApplicationRepository repository;
    @Mock OrderRepository              orderRepository;
    @Mock RestTemplate                 restTemplate;
    @Mock ObjectMapper                 objectMapper;
    @Mock EmailService                 emailService;
    @Mock OrderService                 orderService;

    @InjectMocks TbiLeasingService service;

    private static final String RESELLER_CODE = "BIVD";

    // ── helpers ───────────────────────────────────────────────────────────────

    private LeasingApplication application(String status) {
        LeasingApplication app = new LeasingApplication();
        app.setId(1L);
        app.setTbiOrderId(12345);
        app.setTbiApplicationId("CREDIT-APP-001");
        app.setStoreOrderId("ORD-2026-00001");
        app.setStatus(status);
        app.setProductName("Test Laptop");
        return app;
    }

    private Order pendingOrder() {
        Order order = new Order();
        order.setId(100L);
        order.setOrderNumber("ORD-2026-00001");
        order.setStatus(OrderStatus.LEASING_PENDING);
        order.setCustomerEmail("customer@test.com");
        order.setCustomerFirstName("Ivan");
        order.setCustomerLastName("Petrov");
        return order;
    }

    private TbiStatusWebhookDto webhook(String resellerCode, String orderId,
                                        String creditAppId, String message) {
        TbiStatusWebhookDto dto = new TbiStatusWebhookDto();
        dto.setResellerCode(resellerCode);
        dto.setOrderId(orderId);
        dto.setCreditApplicationId(creditAppId);
        dto.setMessage(message);
        return dto;
    }

    @BeforeEach
    void setUp() {
        when(tbiConfig.getResellerCode()).thenReturn(RESELLER_CODE);
    }

    // ── Сигурност: ResellerCode ───────────────────────────────────────────────

    @Nested
    @DisplayName("ResellerCode валидация")
    class ResellerCodeValidation {

        @Test
        @DisplayName("Грешен ResellerCode → игнориран, без промяна на данни")
        void wrongResellerCode_earlyReturn() {
            service.processStatusWebhook(webhook("WRONG", "12345", null, "ContractSigned"));

            verifyNoInteractions(repository, orderRepository, emailService);
        }

        @Test
        @DisplayName("Правилен ResellerCode (case-insensitive) → обработен")
        void correctResellerCodeCaseInsensitive_processed() {
            LeasingApplication app = application("Draft");
            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook("bivd", "12345", "CREDIT-APP-001", "Approved"));

            verify(repository).save(any());
        }
    }

    // ── Намиране на заявката ──────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveApplication — ред на търсене")
    class ResolveApplication {

        @Test
        @DisplayName("Търси по CreditApplicationId на първо място")
        void byCreditApplicationId_firstPriority() {
            LeasingApplication app = application("Draft");
            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "Approved"));

            verify(repository).findByTbiApplicationId("CREDIT-APP-001");
            verify(repository, never()).findByTbiOrderId(any());
            verify(repository, never()).findByStoreOrderId(any());
        }

        @Test
        @DisplayName("Търси по TBI numeric orderId когато CreditApplicationId не е намерен")
        void byTbiOrderId_whenCreditAppIdMissing() {
            LeasingApplication app = application("Draft");
            when(repository.findByTbiOrderId(12345)).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", null, "Approved"));

            verify(repository, never()).findByTbiApplicationId(any());
            verify(repository).findByTbiOrderId(12345);
        }

        @Test
        @DisplayName("Търси по storeOrderId когато orderId не е число (ORD-XXXX формат)")
        void byStoreOrderId_whenOrderIdIsNotNumeric() {
            LeasingApplication app = application("Draft");
            when(repository.findByStoreOrderId("ORD-2026-00001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook(RESELLER_CODE, "ORD-2026-00001", null, "Approved"));

            verify(repository).findByStoreOrderId("ORD-2026-00001");
        }

        @Test
        @DisplayName("Заявката не е намерена → игнориран webhook, без промяна на данни")
        void notFound_noStateChange() {
            when(repository.findByTbiApplicationId(any())).thenReturn(Optional.empty());
            when(repository.findByTbiOrderId(any())).thenReturn(Optional.empty());
            when(repository.findByStoreOrderId(any())).thenReturn(Optional.empty());

            service.processStatusWebhook(webhook(RESELLER_CODE, "99999", "UNKNOWN", "Approved"));

            verify(repository, never()).save(any());
            verifyNoInteractions(orderRepository, emailService);
        }
    }

    // ── Одобрение: ContractSigned ─────────────────────────────────────────────

    @Nested
    @DisplayName("Одобрение — ContractSigned / approved & signed / Paid")
    class Approval {

        @Test
        @DisplayName("ContractSigned → поръчка PENDING + имейл за потвърждение + нотификация до admin")
        void contractSigned_orderPendingAndEmailsSent() {
            Order order = pendingOrder();
            LeasingApplication app = application("InProgress");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "ContractSigned"));

            assertEquals(OrderStatus.PENDING, order.getStatus());
            verify(emailService).sendOrderConfirmationEmail(order);
            verify(emailService).sendNewOrderNotificationToAdmin(order);
            verify(emailService, never()).sendLeasingRejectedEmail(any(), any());
        }

        @Test
        @DisplayName("approved & signed (BNPL) → поръчка PENDING + имейл за потвърждение")
        void approvedAndSigned_orderPending() {
            Order order = pendingOrder();
            LeasingApplication app = application("Approved");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "approved & signed"));

            assertEquals(OrderStatus.PENDING, order.getStatus());
            verify(emailService).sendOrderConfirmationEmail(order);
        }
    }

    // ── Отхвърляне: Rejected / Canceled / expired ─────────────────────────────

    @Nested
    @DisplayName("Отхвърляне — Rejected / Canceled / expired")
    class Rejection {

        @Test
        @DisplayName("Rejected → поръчка LEASING_REJECTED + имейл за отхвърляне до клиент")
        void rejected_orderLeasingRejectedAndRejectionEmailSent() {
            Order order = pendingOrder();
            LeasingApplication app = application("InProgress");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "Rejected"));

            assertEquals(OrderStatus.LEASING_REJECTED, order.getStatus());
            verify(emailService).sendLeasingRejectedEmail(order, "Test Laptop");
            verify(emailService, never()).sendOrderConfirmationEmail(any());
            verify(emailService, never()).sendNewOrderNotificationToAdmin(any());
        }

        @Test
        @DisplayName("rejected (lowercase — BNPL формат) → LEASING_REJECTED + имейл")
        void rejectedLowercase_orderLeasingRejected() {
            Order order = pendingOrder();
            LeasingApplication app = application("in_progress");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "rejected"));

            assertEquals(OrderStatus.LEASING_REJECTED, order.getStatus());
            verify(emailService).sendLeasingRejectedEmail(order, "Test Laptop");
        }

        @Test
        @DisplayName("Canceled → LEASING_REJECTED + имейл за отхвърляне")
        void canceled_orderLeasingRejected() {
            Order order = pendingOrder();
            LeasingApplication app = application("Approved");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "Canceled"));

            assertEquals(OrderStatus.LEASING_REJECTED, order.getStatus());
            verify(emailService).sendLeasingRejectedEmail(order, "Test Laptop");
        }
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Дублиран webhook (същия статус) → без преход на поръчка, без имейли")
        void duplicateWebhook_sameStatus_noTransition() {
            LeasingApplication app = application("ContractSigned"); // вече ContractSigned
            Order order = pendingOrder();
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "ContractSigned"));

            // status не се е променил → transitionOrderStatus не се вика
            verify(orderRepository, never()).findById(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("Поръчката вече е в PENDING (дублиран ContractSigned) → без втори имейл")
        void orderAlreadyPending_noSecondConfirmation() {
            Order order = pendingOrder();
            order.setStatus(OrderStatus.PENDING); // вече преходирана
            LeasingApplication app = application("Approved");
            app.setOrder(order);

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);
            when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "ContractSigned"));

            assertEquals(OrderStatus.PENDING, order.getStatus());
            verify(emailService, never()).sendOrderConfirmationEmail(any());
            verify(emailService, never()).sendLeasingRejectedEmail(any(), any());
        }

        @Test
        @DisplayName("Заявка без свързана поръчка → без преход, без имейл до клиент")
        void applicationWithoutOrder_noOrderTransition() {
            LeasingApplication app = application("Draft");
            // app.getOrder() == null

            when(repository.findByTbiApplicationId("CREDIT-APP-001")).thenReturn(Optional.of(app));
            when(repository.save(any())).thenReturn(app);

            service.processStatusWebhook(webhook(RESELLER_CODE, "12345", "CREDIT-APP-001", "ContractSigned"));

            verifyNoInteractions(orderRepository);
            verify(emailService, never()).sendOrderConfirmationEmail(any());
            verify(emailService, never()).sendLeasingRejectedEmail(any(), any());
        }

        @Test
        @DisplayName("orderId='0' (express-buy) → не се търси по numeric orderId")
        void orderIdZero_skippedInLookup() {
            // When orderId is "0" and no creditAppId, all lookups are skipped
            service.processStatusWebhook(webhook(RESELLER_CODE, "0", null, "InProgress"));

            verifyNoInteractions(repository, orderRepository, emailService);
        }
    }
}
