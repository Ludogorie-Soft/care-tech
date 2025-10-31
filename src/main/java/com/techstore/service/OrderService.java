package com.techstore.service;

import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.request.UserRequestDTO;
import com.techstore.dto.response.OrderItemResponseDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.dto.speedy.SpeedyCalculatePriceResponse;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.enums.OrderStatus;
import com.techstore.enums.PaymentStatus;
import com.techstore.enums.ShippingMethod;
import com.techstore.event.OrderCreatedEvent;
import com.techstore.event.OrderStatusChangedEvent;
import com.techstore.exception.BusinessLogicException;
import com.techstore.exception.ResourceNotFoundException;
import com.techstore.exception.ValidationException;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.OrderItemRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final SpeedyService speedyService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new order
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO request) {

        if (request.getPassword() != null && request.getConfirmPassword() != null) {

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new BusinessLogicException("Passwords do not match");
            }

            if (userRepository.existsByEmail(request.getCustomerEmail())) {
                throw new BusinessLogicException("User with this email already exists. Please login!");
            }

            UserRequestDTO userRequestDTO = getUserRequestDTO(request);

            try {
                userService.createUser(userRequestDTO);
                log.info("New user created during checkout: {}", request.getCustomerEmail());
            } catch (ValidationException e) {
                throw new BusinessLogicException("Password validation failed: " + e.getMessage());
            }
        }

        User user = userRepository.findByEmail(request.getCustomerEmail()).orElse(null);

        // Create order
        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(generateOrderNumber());
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setShippingMethod(request.getShippingMethod());

        // Set customer information
        order.setCustomerFirstName(request.getCustomerFirstName());
        order.setCustomerLastName(request.getCustomerLastName());
        order.setCustomerEmail(request.getCustomerEmail());
        order.setCustomerPhone(request.getCustomerPhone());
        order.setCustomerCompany(request.getCustomerCompany());
        order.setCustomerVatNumber(request.getCustomerVatNumber());
        order.setCustomerVatRegistered(request.getCustomerVatRegistered());

        order.setIsToSpeedyOffice(request.getIsToSpeedyOffice());

        order.setInsuranceOffer(request.getInsuranceOffer());
        order.setInstallmentOffer(request.getInstallmentOffer());
        order.setTermsAgreed(request.getTermsAgreed());

        // Shipping address
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingPostalCode(request.getShippingPostalCode());
        order.setShippingCountry(request.getShippingCountry());

        // Speedy specific fields
        if (request.getShippingMethod() == ShippingMethod.SPEEDY) {
            order.setShippingSpeedySiteId(request.getShippingSpeedySiteId());
            order.setShippingSpeedyOfficeId(request.getShippingSpeedyOfficeId());
            order.setShippingSpeedySiteName(request.getShippingSpeedySiteName());
            order.setShippingSpeedyOfficeName(request.getShippingSpeedyOfficeName());
        }

        // Billing address
        if (Boolean.TRUE.equals(request.getUseSameAddressForBilling())) {
            order.setBillingAddress(request.getShippingAddress());
            order.setBillingCity(request.getShippingCity());
            order.setBillingPostalCode(request.getShippingPostalCode());
            order.setBillingCountry(request.getShippingCountry());
        } else {
            order.setBillingAddress(request.getBillingAddress());
            order.setBillingCity(request.getBillingCity());
            order.setBillingPostalCode(request.getBillingPostalCode());
            order.setBillingCountry(request.getBillingCountry());
        }

        // Notes and shipping cost
        order.setCustomerNotes(request.getCustomerNotes());
//        order.setShippingCost(shippingCost);

        // Add order items
        for (var itemDto : request.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemDto.getProductId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getNameBg());
            orderItem.setProductSku(product.getSku());
            orderItem.setProductModel(product.getModel());
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setUnitPrice(product.getPriceClient()); // Price without VAT
            orderItem.setTaxRate(new BigDecimal("20.00")); // 20% VAT

            orderItem.calculateLineTotals();
            order.addOrderItem(orderItem);
        }

        // Calculate totals
        order.calculateTotals();

        order.setShippingCost(request.getShippingCost());

        // Save order
        order = orderRepository.save(order);

        // Clear user's cart
        cartItemRepository.deleteByUserEmail(request.getCustomerEmail());

        log.info("Order created successfully: {}", order.getOrderNumber());

        // Publish order created event
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order));

        return mapToResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponseDTO(order);
    }

    /**
     * Get order by order number
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponseDTO(order);
    }

    /**
     * Get user's orders
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getUserOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(this::mapToResponseDTO);
    }

    /**
     * Get all orders (admin)
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getAllOrders(Pageable pageable) {
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(this::mapToResponseDTO);
    }

    /**
     * Get orders by status
     */
    @Transactional(readOnly = true)
    public Page<OrderResponseDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return orders.map(this::mapToResponseDTO);
    }

    /**
     * Update order status
     */
    @Transactional
    public OrderResponseDTO updateOrderStatus(Long orderId, OrderStatusUpdateDTO request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(request.getStatus());

        if (request.getAdminNotes() != null) {
            order.setAdminNotes(request.getAdminNotes());
        }

        if (request.getTrackingNumber() != null) {
            order.setTrackingNumber(request.getTrackingNumber());
        }

        // Automatic date setting
        if (request.getStatus() == OrderStatus.SHIPPED && order.getShippedAt() == null) {
            order.setShippedAt(LocalDateTime.now());
        }

        if (request.getStatus() == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setPaymentStatus(PaymentStatus.PAID); // Auto-mark as paid when delivered
        }

        order = orderRepository.save(order);

        log.info("Order {} status updated from {} to {}",
                order.getOrderNumber(), previousStatus, request.getStatus());

        // Publish order status changed event
        eventPublisher.publishEvent(new OrderStatusChangedEvent(this, order, previousStatus, request.getStatus()));

        return mapToResponseDTO(order);
    }

    /**
     * Update payment status
     */
    @Transactional
    public OrderResponseDTO updatePaymentStatus(Long orderId, PaymentStatus paymentStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setPaymentStatus(paymentStatus);
        order = orderRepository.save(order);

        log.info("Order {} payment status updated to {}", order.getOrderNumber(), paymentStatus);

        return mapToResponseDTO(order);
    }

    /**
     * Cancel order
     */
    @Transactional
    public OrderResponseDTO cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel shipped order. Please contact customer service.");
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        String cancellationNote = "Cancellation reason: " + reason + " (at: " + LocalDateTime.now() + ")";
        order.setAdminNotes(order.getAdminNotes() != null ?
                order.getAdminNotes() + "\n" + cancellationNote : cancellationNote);

        order = orderRepository.save(order);

        log.info("Order {} cancelled. Reason: {}", order.getOrderNumber(), reason);

        // Publish order status changed event
        eventPublisher.publishEvent(new OrderStatusChangedEvent(this, order, previousStatus, OrderStatus.CANCELLED));

        return mapToResponseDTO(order);
    }

    /**
     * Get orders for specific month (for NAP file)
     */
    @Transactional(readOnly = true)
    public List<Order> getOrdersForMonth(int year, int month) {
        return orderRepository.findOrdersByYearAndMonth(year, month);
    }

    /**
     * Get order statistics
     */
    @Transactional(readOnly = true)
    public OrderStatistics getOrderStatistics() {
        OrderStatistics stats = new OrderStatistics();
        stats.setTotalOrders(orderRepository.count());
        stats.setPendingOrders(orderRepository.countByStatus(OrderStatus.PENDING));
        stats.setShippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED));
        stats.setDeliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED));
        stats.setCancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED));
        stats.setTotalRevenue(orderRepository.sumTotalAmount());

        return stats;
    }

    /**
     * Generate unique order number
     */
    private String generateOrderNumber() {
        String year = String.valueOf(Year.now().getValue());
        long count = orderRepository.count() + 1;
        return String.format("ORD-%s-%05d", year, count);
    }

    /**
     * Map Order to OrderResponseDTO
     */
    private OrderResponseDTO mapToResponseDTO(Order order) {
        List<OrderItemResponseDTO> items = order.getOrderItems().stream()
                .map(this::mapItemToResponseDTO)
                .collect(Collectors.toList());

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerFirstName(order.getCustomerFirstName())
                .customerLastName(order.getCustomerLastName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .customerCompany(order.getCustomerCompany())
                .customerVatNumber(order.getCustomerVatNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .paymentMethod(order.getPaymentMethod())
                .shippingMethod(order.getShippingMethod())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .shippingCost(order.getShippingCost())
                .discountAmount(order.getDiscountAmount())
                .total(order.getTotal())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingPostalCode(order.getShippingPostalCode())
                .shippingCountry(order.getShippingCountry())
                .billingAddress(order.getBillingAddress())
                .billingCity(order.getBillingCity())
                .billingPostalCode(order.getBillingPostalCode())
                .billingCountry(order.getBillingCountry())
                .shippingSpeedySiteId(order.getShippingSpeedySiteId())
                .shippingSpeedyOfficeId(order.getShippingSpeedyOfficeId())
                .shippingSpeedySiteName(order.getShippingSpeedySiteName())
                .shippingSpeedyOfficeName(order.getShippingSpeedyOfficeName())
                .items(items)
                .customerNotes(order.getCustomerNotes())
                .adminNotes(order.getAdminNotes())
                .trackingNumber(order.getTrackingNumber())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .invoiceNumber(order.getInvoiceNumber())
                .invoiceDate(order.getInvoiceDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .isToSpeedyOffice(order.getIsToSpeedyOffice())
                .insuranceOffer(order.getInsuranceOffer())
                .installmentOffer(order.getInstallmentOffer())
                .termsAgreed(order.getTermsAgreed())
                .shippingCost(order.getShippingCost())
                .build();
    }

    private OrderItemResponseDTO mapItemToResponseDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .productModel(item.getProductModel())
                .productImageUrl(item.getProduct().getPrimaryImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxRate(item.getTaxRate())
                .lineTotal(item.getLineTotal())
                .lineTax(item.getLineTax())
                .discountAmount(item.getDiscountAmount())
                .build();
    }

    private UserRequestDTO getUserRequestDTO(OrderCreateRequestDTO request) {
        UserRequestDTO userRequestDTO = new UserRequestDTO();
        userRequestDTO.setFirstName(request.getCustomerFirstName());
        userRequestDTO.setLastName(request.getCustomerLastName());
        userRequestDTO.setPhone(request.getCustomerPhone());
        userRequestDTO.setEmail(request.getCustomerEmail());
        userRequestDTO.setRole(String.valueOf(User.Role.USER));
        userRequestDTO.setUsername(request.getCustomerEmail());
        userRequestDTO.setPassword(request.getPassword());
        userRequestDTO.setActive(true);
        return userRequestDTO;
    }

    /**
     * Inner class for order statistics
     */
    @Data
    public static class OrderStatistics {
        private Long totalOrders;
        private Long pendingOrders;
        private Long shippedOrders;
        private Long deliveredOrders;
        private Long cancelledOrders;
        private BigDecimal totalRevenue;
    }
}