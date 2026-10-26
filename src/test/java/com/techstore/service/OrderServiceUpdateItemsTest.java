package com.techstore.service;

import com.techstore.config.ShippingConfig;
import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.request.OrderItemUpdateDTO;
import com.techstore.dto.request.OrderUpdateRequestDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.entity.Order;
import com.techstore.entity.OrderItem;
import com.techstore.entity.Product;
import com.techstore.exception.BusinessLogicException;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.OrderRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService.updateOrder — цени и добавяне на продукти")
class OrderServiceUpdateItemsTest {

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
    private OrderItem camera;

    // ── helpers ───────────────────────────────────────────────────────────────

    private Product product(long id, String name, String finalPrice, String discount) {
        Product p = new Product();
        p.setId(id);
        p.setNameBg(name);
        p.setSku("SKU-" + id);
        p.setFinalPrice(new BigDecimal(finalPrice));
        p.setDiscount(new BigDecimal(discount));
        return p;
    }

    private OrderCreateRequestDTO.OrderItemRequestDTO newItem(long productId, int quantity, String customPrice) {
        OrderCreateRequestDTO.OrderItemRequestDTO dto = new OrderCreateRequestDTO.OrderItemRequestDTO();
        dto.setProductId(productId);
        dto.setQuantity(quantity);
        dto.setCustomPriceEuro(customPrice != null ? new BigDecimal(customPrice) : null);
        return dto;
    }

    private OrderItemUpdateDTO itemUpdate(int quantity, String unitPrice) {
        OrderItemUpdateDTO dto = new OrderItemUpdateDTO();
        dto.setOrderItemId(camera.getId());
        dto.setQuantity(quantity);
        dto.setUnitPrice(unitPrice != null ? new BigDecimal(unitPrice) : null);
        return dto;
    }

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-2026-00001");
        order.setShippingCost(new BigDecimal("3.50"));

        camera = new OrderItem();
        camera.setId(10L);
        camera.setProduct(product(100L, "Камера", "100.00", "10.00"));
        camera.setProductName("Камера");
        camera.setQuantity(1);
        camera.setUnitPrice(new BigDecimal("100.00"));
        camera.setDiscountAmount(new BigDecimal("10.00"));
        camera.calculateLineTotals();
        order.addOrderItem(camera);
        order.calculateTotals();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Нова цена на артикул — нулира отстъпката и преизчислява сумите")
    void changedUnitPriceResetsDiscountAndRecalculatesTotals() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setItems(List.of(itemUpdate(2, "80.00")));

        OrderResponseDTO response = service.updateOrder(1L, request);

        assertEquals(new BigDecimal("80.00"), camera.getUnitPrice());
        assertEquals(BigDecimal.ZERO, camera.getDiscountAmount());
        assertEquals(new BigDecimal("160.00"), response.getSubtotal());
        assertEquals(new BigDecimal("32.00"), response.getTaxAmount());
        assertEquals(new BigDecimal("195.50"), response.getTotal());
    }

    @Test
    @DisplayName("Същата цена или липсваща цена — отстъпката остава")
    void unchangedUnitPriceKeepsDiscount() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setItems(List.of(itemUpdate(1, "100.00")));

        service.updateOrder(1L, request);

        assertEquals(new BigDecimal("10.00"), camera.getDiscountAmount());

        request.setItems(List.of(itemUpdate(3, null)));
        service.updateOrder(1L, request);

        assertEquals(new BigDecimal("100.00"), camera.getUnitPrice());
        assertEquals(new BigDecimal("10.00"), camera.getDiscountAmount());
        assertEquals(3, camera.getQuantity());
    }

    @Test
    @DisplayName("Добавен продукт без цена — взима текущата цена и отстъпка на продукта")
    void newItemWithoutPriceUsesProductPrice() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product(200L, "MicroSD 128GB", "15.00", "5.00")));

        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setNewItems(List.of(newItem(200L, 1, null)));

        OrderResponseDTO response = service.updateOrder(1L, request);

        assertEquals(2, order.getOrderItems().size());
        OrderItem card = order.getOrderItems().get(1);
        assertEquals("MicroSD 128GB", card.getProductName());
        assertEquals(new BigDecimal("15.00"), card.getUnitPrice());
        assertEquals(new BigDecimal("5.00"), card.getDiscountAmount());
        assertEquals(new BigDecimal("115.00"), response.getSubtotal());
        assertEquals(new BigDecimal("141.50"), response.getTotal());
    }

    @Test
    @DisplayName("Добавен продукт с цена от админа — без отстъпка")
    void newItemWithCustomPriceHasNoDiscount() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product(200L, "MicroSD 128GB", "15.00", "5.00")));

        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setNewItems(List.of(newItem(200L, 2, "12.50")));

        service.updateOrder(1L, request);

        OrderItem card = order.getOrderItems().get(1);
        assertEquals(new BigDecimal("12.50"), card.getUnitPrice());
        assertEquals(BigDecimal.ZERO, card.getDiscountAmount());
        assertEquals(new BigDecimal("25.00"), card.getLineTotal());
    }

    @Test
    @DisplayName("Премахнат е единственият продукт, но е добавен нов — поръчката е валидна")
    void replacingTheOnlyItemIsAllowed() {
        when(productRepository.findById(200L))
                .thenReturn(Optional.of(product(200L, "MicroSD 128GB", "15.00", "0.00")));

        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setItems(List.of(itemUpdate(0, null)));
        request.setNewItems(List.of(newItem(200L, 1, null)));

        service.updateOrder(1L, request);

        assertEquals(1, order.getOrderItems().size());
        assertEquals("MicroSD 128GB", order.getOrderItems().get(0).getProductName());
    }

    @Test
    @DisplayName("Премахнат е единственият продукт без нов — грешка")
    void removingEveryItemIsRejected() {
        OrderUpdateRequestDTO request = new OrderUpdateRequestDTO();
        request.setItems(List.of(itemUpdate(0, null)));

        assertThrows(BusinessLogicException.class, () -> service.updateOrder(1L, request));
    }
}
