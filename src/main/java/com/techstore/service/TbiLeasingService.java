package com.techstore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.config.TbiConfig;
import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.dto.tbi.*;
import com.techstore.entity.LeasingApplication;
import com.techstore.entity.Order;
import com.techstore.entity.User;
import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentMethod;
import com.techstore.enums.PaymentStatus;
import com.techstore.repository.LeasingApplicationRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.util.TbiEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TbiLeasingService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // Final statuses that trigger an admin email notification
    private static final Set<String> NOTIFY_STATUSES = Set.of(
            "Approved", "Rejected", "ContractSigned", "Paid",
            "Canceled", "approved & signed", "rejected", "canceled"
    );

    // TBI statuses that mean the contract is signed — transition order to PENDING (ready to ship)
    private static final Set<String> CONTRACT_SIGNED_STATUSES = Set.of(
            "ContractSigned", "approved & signed", "Paid"
    );

    // TBI statuses that mean the application was declined — cancel the order
    private static final Set<String> DECLINED_STATUSES = Set.of(
            "Rejected", "rejected", "Canceled", "canceled", "expired"
    );

    private final TbiConfig                    tbiConfig;
    private final LeasingApplicationRepository repository;
    private final OrderRepository              orderRepository;
    private final RestTemplate                 restTemplate;
    private final ObjectMapper                 objectMapper;
    private final EmailService                 emailService;
    private final OrderService                 orderService;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initiates a TBI leasing application for a product-page "Buy with TBI" click.
     * Uses orderid=0 (express-buy mode) so TBI handles the order creation flow.
     *
     * @param request  product details from the frontend
     * @param user     the currently authenticated user (may be null for guest — admin-only for now)
     * @return DTO containing our internal applicationId and the TBI iframe URL
     */
    @Transactional
    public TbiRegisterResponseDto registerApplication(TbiRegisterRequestDto request, User user) {
        if (!tbiConfig.isEnabled()) {
            throw new IllegalStateException("TBI integration is disabled");
        }

        List<TbiItemDto> items = buildItems(request);
        String itemsJson = serializeQuietly(items);

        TbiApplicationDataDto data = TbiApplicationDataDto.builder()
                .orderid("0")
                .firstname(user != null ? user.getFirstName() : "")
                .lastname(user  != null ? user.getLastName()  : "")
                .surname("")
                .email(user   != null ? user.getEmail()     : "")
                .phone(normalizePhone(user))
                .deliveryaddress(TbiDeliveryAddressDto.empty())
                .items(items)
                .statusURL(tbiConfig.getWebhookUrl())
                .build();

        String encryptedData = TbiEncryptionUtil.encrypt(
                serializeQuietly(data),
                tbiConfig.getEncryptionKey()
        );

        Map<String, String> requestBody = Map.of(
                "reseller_code", tbiConfig.getResellerCode(),
                "reseller_key",  tbiConfig.getResellerKey(),
                "data",          encryptedData
        );

        TbiRegisterApplicationResponseDto tbiResponse = callTbi(
                tbiConfig.getRegisterApplicationUrl(),
                requestBody,
                TbiRegisterApplicationResponseDto.class
        );

        if (!tbiResponse.isSuccess()) {
            log.error("TBI RegisterApplication failed: error={} message={}",
                    tbiResponse.getError(), tbiResponse.getMessage());
            throw new IllegalStateException(
                    "TBI registration failed: " + tbiResponse.getMessage());
        }

        // ── Build customer info ─────────────────────────────────────────────
        String firstName = user != null ? user.getFirstName() : "";
        String lastName  = user != null ? user.getLastName()  : "";
        String email     = user != null ? user.getEmail()     : "";
        String phone     = normalizePhone(user);

        // ── Create order immediately with LEASING_PENDING status ────────────
        OrderCreateRequestDTO orderRequest = buildOrderRequest(request, firstName, lastName, email, phone);
        OrderResponseDTO createdOrder = orderService.createOrder(orderRequest);

        Order orderEntity = orderRepository.findById(createdOrder.getId())
                .orElseThrow(() -> new IllegalStateException("Order not found after creation"));

        // ── Save leasing application ────────────────────────────────────────
        LeasingApplication application = new LeasingApplication();
        application.setOrder(orderEntity);
        application.setTbiOrderId(tbiResponse.getOrderId());
        application.setTbiToken(tbiResponse.getToken());
        application.setStoreOrderId(createdOrder.getOrderNumber());
        application.setStatus("Draft");
        if (isCartOrder(request)) {
            // Cart: total EUR = sum of (priceEuro * quantity) across all items
            double totalEur = request.getCartItems().stream()
                    .mapToDouble(ci -> (ci.getPriceEuro() != null ? ci.getPriceEuro() : 0)
                            * (ci.getQuantity() != null ? ci.getQuantity() : 1))
                    .sum();
            application.setAmountEur(BigDecimal.valueOf(totalEur));
            application.setProductId(null);
            application.setProductName("Кошница (" + request.getCartItems().size() + " продукта)");
        } else {
            application.setAmountEur(request.getPriceEuro() != null
                    ? BigDecimal.valueOf(request.getPriceEuro()) : null);
            application.setProductId(request.getProductId());
            application.setProductName(request.getProductName());
        }
        application.setItemsJson(itemsJson);
        application.setTbiRawResponse(serializeQuietly(tbiResponse));
        application.setStatusHistory(buildInitialHistory());

        application.setCustomerFirstName(firstName);
        application.setCustomerLastName(lastName);
        application.setCustomerPhone(phone);
        application.setCustomerEmail(email);

        // Shipping
        application.setShippingAddress(request.getShippingAddress());
        application.setShippingCity(request.getShippingCity());
        application.setShippingPostalCode(request.getShippingPostalCode());
        application.setIsToSpeedyOffice(request.getIsToSpeedyOffice());
        application.setShippingSpeedySiteId(request.getShippingSpeedySiteId());
        application.setShippingSpeedyOfficeId(request.getShippingSpeedyOfficeId());
        application.setShippingSpeedySiteName(request.getShippingSpeedySiteName());
        application.setShippingSpeedyOfficeName(request.getShippingSpeedyOfficeName());

        LeasingApplication saved = repository.save(application);
        log.info("TBI leasing application created: id={} tbiOrderId={} orderId={}",
                saved.getId(), saved.getTbiOrderId(), orderEntity.getId());

        return new TbiRegisterResponseDto(saved.getId(), tbiResponse.getUrl());
    }

    /**
     * Processes an incoming status webhook from TBI.
     * Updates the application status and notifies the admin on significant changes.
     */
    @Transactional
    public void processStatusWebhook(TbiStatusWebhookDto payload) {
        log.info("TBI webhook received: orderId={} status={}",
                payload.getOrderId(), payload.resolvedStatus());

        // Guard against spoofed webhooks — TBI always includes the reseller code
        if (!tbiConfig.getResellerCode().equalsIgnoreCase(payload.getResellerCode())) {
            log.warn("TBI webhook rejected: unexpected reseller_code='{}', expected='{}'",
                    payload.getResellerCode(), tbiConfig.getResellerCode());
            return;
        }

        LeasingApplication application = resolveApplication(payload);
        if (application == null) {
            log.warn("TBI webhook: no application found for orderId={} creditAppId={}",
                    payload.getOrderId(), payload.getCreditApplicationId());
            return;
        }

        String newStatus = payload.resolvedStatus();
        String previousStatus = application.getStatus();

        application.setStatus(newStatus);
        application.setWebhookReceivedAt(LocalDateTime.now());

        if (payload.getCreditApplicationId() != null) {
            application.setTbiApplicationId(payload.getCreditApplicationId());
        }

        application.setStatusHistory(appendHistory(application.getStatusHistory(), newStatus));

        repository.save(application);

        log.info("TBI application {} status: {} → {}", application.getId(), previousStatus, newStatus);

        // ── Auto-transition the linked order ────────────────────────────────
        if (!newStatus.equals(previousStatus) && application.getOrder() != null) {
            transitionOrderStatus(application, newStatus);
        }

        if (!newStatus.equals(previousStatus) && NOTIFY_STATUSES.contains(newStatus)) {
            sendAdminNotification(application, newStatus);
        }
    }

    private void transitionOrderStatus(LeasingApplication application, String tbiStatus) {
        Order order = orderRepository.findById(application.getOrder().getId()).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.LEASING_PENDING) return;

        if (CONTRACT_SIGNED_STATUSES.contains(tbiStatus)) {
            order.setStatus(OrderStatus.PENDING);
            if ("Paid".equals(tbiStatus)) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
            orderRepository.save(order);
            log.info("Order {} transitioned to PENDING after TBI status '{}'",
                    order.getOrderNumber(), tbiStatus);
            // Send the confirmation email that was withheld at order creation
            try {
                emailService.sendOrderConfirmationEmail(order);
                emailService.sendNewOrderNotificationToAdmin(order);
            } catch (Exception e) {
                log.error("Failed to send order confirmation after leasing approval: {}", e.getMessage());
            }
        } else if (DECLINED_STATUSES.contains(tbiStatus)) {
            order.setStatus(OrderStatus.LEASING_REJECTED);
            orderRepository.save(order);
            log.info("Order {} marked LEASING_REJECTED after TBI status '{}'",
                    order.getOrderNumber(), tbiStatus);
        }
    }

    /**
     * Fetches the latest status from TBI for a given application (polling fallback).
     * Use only when statusURL webhook is not configured.
     */
    public LeasingApplicationResponseDto getApplicationStatus(Long applicationId) {
        LeasingApplication application = repository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Leasing application not found: " + applicationId));

        if (application.getTbiOrderId() == null || application.getTbiToken() == null) {
            return LeasingApplicationResponseDto.from(application);
        }

        try {
            Map<String, Object> requestBody = Map.of(
                    "reseller_code", tbiConfig.getResellerCode(),
                    "reseller_key",  tbiConfig.getResellerKey(),
                    "order_id",      application.getTbiOrderId(),
                    "token",         application.getTbiToken()
            );

            Map<?, ?> response = callTbi(
                    tbiConfig.getGetApplicationDataUrl(),
                    requestBody,
                    Map.class
            );

            if (response != null && response.get("status") != null) {
                String newStatus = response.get("status").toString();
                if (!newStatus.equals(application.getStatus())) {
                    application.setStatus(newStatus);
                    application.setStatusHistory(appendHistory(application.getStatusHistory(), newStatus));
                    if (response.get("installment") != null) {
                        application.setInstallment(new BigDecimal(response.get("installment").toString()));
                    }
                    if (response.get("period") != null) {
                        application.setPeriodMonths(Integer.parseInt(response.get("period").toString()));
                    }
                    repository.save(application);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to poll TBI application status for id={}: {}", applicationId, e.getMessage());
        }

        return LeasingApplicationResponseDto.from(application);
    }

    private OrderCreateRequestDTO buildOrderRequest(TbiRegisterRequestDto request,
                                                    String firstName, String lastName,
                                                    String email, String phone) {
        OrderCreateRequestDTO req = new OrderCreateRequestDTO();

        req.setCustomerFirstName(firstName);
        req.setCustomerLastName(lastName);
        req.setCustomerEmail(email);
        req.setCustomerPhone(phone);

        String country = request.getShippingCountry() != null ? request.getShippingCountry() : "Bulgaria";

        req.setShippingAddress(request.getShippingAddress());
        req.setShippingCity(request.getShippingCity());
        req.setShippingPostalCode(request.getShippingPostalCode());
        req.setShippingCountry(country);
        req.setShippingMethod(request.getShippingMethod());
        req.setIsToSpeedyOffice(Boolean.TRUE.equals(request.getIsToSpeedyOffice()));
        req.setShippingSpeedySiteId(request.getShippingSpeedySiteId());
        req.setShippingSpeedyOfficeId(request.getShippingSpeedyOfficeId());
        req.setShippingSpeedySiteName(request.getShippingSpeedySiteName());
        req.setShippingSpeedyOfficeName(request.getShippingSpeedyOfficeName());

        // Billing mirrors shipping for leasing orders — customer fills billing during TBI flow
        req.setUseSameAddressForBilling(true);
        req.setBillingAddress(request.getShippingAddress());
        req.setBillingCity(request.getShippingCity());
        req.setBillingPostalCode(request.getShippingPostalCode());
        req.setBillingCountry(country);

        req.setPaymentMethod(PaymentMethod.TBI_LEASING);
        req.setInsuranceOffer(false);
        req.setInstallmentOffer(false);
        req.setTermsAgreed(true);
        req.setIsLeasingOrder(true);   // ← signals OrderService: LEASING_PENDING, skip emails

        if (isCartOrder(request)) {
            List<OrderCreateRequestDTO.OrderItemRequestDTO> orderItems = request.getCartItems().stream()
                    .map(ci -> {
                        if (ci.getProductId() == null) {
                            throw new IllegalStateException("productId is required for each cart item");
                        }
                        OrderCreateRequestDTO.OrderItemRequestDTO oi = new OrderCreateRequestDTO.OrderItemRequestDTO();
                        oi.setProductId(ci.getProductId());
                        oi.setQuantity(ci.getQuantity() != null ? ci.getQuantity() : 1);
                        return oi;
                    })
                    .toList();
            req.setItems(orderItems);
        } else {
            if (request.getProductId() == null) {
                throw new IllegalStateException("productId is required for leasing order");
            }
            OrderCreateRequestDTO.OrderItemRequestDTO item = new OrderCreateRequestDTO.OrderItemRequestDTO();
            item.setProductId(request.getProductId());
            item.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
            req.setItems(List.of(item));
        }

        return req;
    }

    // ── Admin queries ─────────────────────────────────────────────────────────

    public Page<LeasingApplicationResponseDto> getAllApplications(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
                .map(LeasingApplicationResponseDto::from);
    }

    public Page<LeasingApplicationResponseDto> getApplicationsByStatus(String status, Pageable pageable) {
        return repository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(LeasingApplicationResponseDto::from);
    }

    public LeasingApplicationResponseDto getApplicationById(Long id) {
        return repository.findById(id)
                .map(LeasingApplicationResponseDto::from)
                .orElseThrow(() -> new NoSuchElementException("Leasing application not found: " + id));
    }

    public LeasingApplicationStatisticsDto getStatistics() {
        return LeasingApplicationStatisticsDto.builder()
                .total(repository.count())
                .draft(repository.countByStatus("Draft"))
                .inProgress(repository.countByStatus("InProgress")
                        + repository.countByStatus("InProgressAssessment")
                        + repository.countByStatus("MP Sent")
                        + repository.countByStatus("KYC")
                        + repository.countByStatus("ContractSigning")
                        + repository.countByStatus("in_progress"))
                .approved(repository.countByStatus("Approved"))
                .contractSigned(repository.countByStatus("ContractSigned")
                        + repository.countByStatus("approved & signed"))
                .paid(repository.countByStatus("Paid"))
                .rejected(repository.countByStatus("Rejected")
                        + repository.countByStatus("rejected"))
                .canceled(repository.countByStatus("Canceled")
                        + repository.countByStatus("canceled")
                        + repository.countByStatus("expired"))
                .totalSignedAmountEur(repository.sumSignedAmountEur())
                .totalApprovedAmountEur(repository.sumApprovedAmountEur())
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<TbiItemDto> buildItems(TbiRegisterRequestDto request) {
        if (isCartOrder(request)) {
            return request.getCartItems().stream()
                    .map(ci -> TbiItemDto.builder()
                            .name(ci.getProductName() != null ? ci.getProductName() : "Продукт")
                            .description("")
                            .qty(String.valueOf(ci.getQuantity() != null ? ci.getQuantity() : 1))
                            .price(ci.getPriceEuro() != null
                                    ? String.format(Locale.US, "%.2f", ci.getPriceEuro()) : "0.00")
                            .sku(ci.getSku() != null ? ci.getSku() : "")
                            .category("0")
                            .imagelink(ci.getImageUrl() != null ? ci.getImageUrl() : "")
                            .build())
                    .toList();
        }
        return List.of(TbiItemDto.builder()
                .name(request.getProductName() != null ? request.getProductName() : "Продукт")
                .description("")
                .qty(String.valueOf(request.getQuantity() != null ? request.getQuantity() : 1))
                .price(request.getPriceEuro() != null
                        ? String.format(Locale.US, "%.2f", request.getPriceEuro()) : "0.00")
                .sku(request.getSku() != null ? request.getSku() : "")
                .category("0")
                .imagelink(request.getImageUrl() != null ? request.getImageUrl() : "")
                .build());
    }

    private boolean isCartOrder(TbiRegisterRequestDto request) {
        return request.getCartItems() != null && !request.getCartItems().isEmpty();
    }

    private LeasingApplication resolveApplication(TbiStatusWebhookDto payload) {
        // 1. Try by CreditApplicationId — most specific, present on all BNPL and most standard webhooks
        if (payload.getCreditApplicationId() != null) {
            Optional<LeasingApplication> byAppId =
                    repository.findByTbiApplicationId(payload.getCreditApplicationId());
            if (byAppId.isPresent()) return byAppId.get();
        }

        // 2. Try by TBI's own numeric order_id (returned in RegisterApplication and echoed in webhooks).
        //    We send orderid="0" for express-buy, so TBI assigns their own id — skip "0".
        if (payload.getOrderId() != null && !"0".equals(payload.getOrderId())) {
            try {
                Integer tbiOid = Integer.parseInt(payload.getOrderId());
                Optional<LeasingApplication> byTbiOrderId = repository.findByTbiOrderId(tbiOid);
                if (byTbiOrderId.isPresent()) return byTbiOrderId.get();
            } catch (NumberFormatException ignored) {
                // Non-numeric OrderId — fall through to store order lookup
            }

            // 3. Try by our store order number (ORD-XXXX) — used when orderid != "0"
            Optional<LeasingApplication> byStoreOrder =
                    repository.findByStoreOrderId(payload.getOrderId());
            if (byStoreOrder.isPresent()) return byStoreOrder.get();
        }

        return null;
    }

    private String buildInitialHistory() {
        return serializeQuietly(List.of(Map.of(
                "datetime", LocalDateTime.now().format(DT_FMT),
                "status",   "Draft"
        )));
    }

    private String appendHistory(String existingJson, String newStatus) {
        try {
            List<Map<String, String>> history = existingJson != null
                    ? new ArrayList<>(Arrays.asList(
                            objectMapper.readValue(existingJson, Map[].class)).stream()
                            .map(m -> (Map<String, String>) m)
                            .toList())
                    : new ArrayList<>();

            history.add(Map.of(
                    "datetime", LocalDateTime.now().format(DT_FMT),
                    "status",   newStatus
            ));

            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException e) {
            log.warn("Failed to append status history: {}", e.getMessage());
            return existingJson;
        }
    }

    private <T> T callTbi(String url, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        ResponseEntity<T> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, responseType);

        T responseBody = response.getBody();
        if (responseBody == null) {
            throw new IllegalStateException(
                    "TBI API returned an empty response body from: " + url);
        }
        return responseBody;
    }

    /**
     * Normalises a phone number to Bulgarian format with a leading "0".
     * Handles numbers stored with or without the prefix.
     */
    private static String normalizePhone(User user) {
        if (user == null || user.getPhone() == null) return "";
        String raw = user.getPhone().trim();
        return raw.startsWith("0") ? raw : "0" + raw;
    }

    private String serializeQuietly(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Serialization failed: {}", e.getMessage());
            return "{}";
        }
    }

    private void sendAdminNotification(LeasingApplication application, String status) {
        try {
            String subject = String.format("[CareTech] TBI лизинг — %s | %s",
                    status, application.getProductName());

            String body = String.format(
                    "Лизингова заявка #%d е сменила статус на: <strong>%s</strong><br><br>" +
                    "Продукт: %s<br>" +
                    "Клиент: %s %s<br>" +
                    "Сума: %s EUR<br>" +
                    "TBI ID: %s<br><br>" +
                    "Вижте детайли в <a href='https://caretech.bg/admin/dashboard/leasing'>Админ панел → Лизинг</a>",
                    application.getId(),
                    status,
                    application.getProductName(),
                    application.getCustomerFirstName() != null ? application.getCustomerFirstName() : "",
                    application.getCustomerLastName()  != null ? application.getCustomerLastName()  : "",
                    application.getAmountEur() != null ? application.getAmountEur().toPlainString() : "N/A",
                    application.getTbiApplicationId() != null ? application.getTbiApplicationId() : "N/A"
            );

            emailService.sendHtmlEmail("info@caretech.bg", subject, body);
            log.info("Admin notification sent for leasing application {} status {}", application.getId(), status);
        } catch (Exception e) {
            log.error("Failed to send admin notification for leasing application {}: {}",
                    application.getId(), e.getMessage());
        }
    }
}
