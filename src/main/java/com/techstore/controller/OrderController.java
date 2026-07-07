package com.techstore.controller;

import com.techstore.dto.request.OrderCreateRequestDTO;
import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.entity.User;
import com.techstore.service.OrderService;
import com.techstore.service.S3Service;
import com.techstore.util.SecurityHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final SecurityHelper securityHelper;
    private final S3Service s3Service;

    /**
     * Създаване на нова поръчка
     */
    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderCreateRequestDTO request) {
        // Security: strip admin-only fields — this endpoint is permitAll (guest checkout)
        if (request.getItems() != null) {
            request.getItems().forEach(item -> item.setCustomPriceEuro(null));
        }
        request.setSkipCartClear(false);
        OrderResponseDTO order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Взема поръчка по ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @PathVariable Long orderId) {

        User currentUser = securityHelper.getCurrentUser();
        OrderResponseDTO order = orderService.getOrderById(orderId);

        if (!currentUser.isAdmin() && !ownsOrder(currentUser, order)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Взема поръчка по номер
     */
    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderResponseDTO> getOrderByNumber(
            @PathVariable String orderNumber) {

        User currentUser = securityHelper.getCurrentUser();
        OrderResponseDTO order = orderService.getOrderByNumber(orderNumber);

        if (!currentUser.isAdmin() && !ownsOrder(currentUser, order)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(order);
    }

    /**
     * Взема поръчките на текущия потребител
     */
    @GetMapping("/my-orders")
    public ResponseEntity<Page<OrderResponseDTO>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User currentUser = securityHelper.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderResponseDTO> orders = orderService.getUserOrders(currentUser.getId(), pageable);
        return ResponseEntity.ok(orders);
    }

    /**
     * Сваля гаранционния файл на поръчката (само собствени поръчки)
     */
    @GetMapping("/{orderId}/warranty/download")
    public ResponseEntity<byte[]> downloadWarrantyFile(@PathVariable Long orderId) {
        User currentUser = securityHelper.getCurrentUser();
        OrderResponseDTO order = orderService.getOrderById(orderId);

        if (!currentUser.isAdmin() && !ownsOrder(currentUser, order)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (order.getWarrantyFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        String key = order.getWarrantyFilePath();
        byte[] fileBytes = s3Service.downloadFileBytes(key);
        String filename = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        String contentType = switch (extension) {
            case "pdf"  -> "application/pdf";
            case "doc"  -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls"  -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "odt"  -> "application/vnd.oasis.opendocument.text";
            case "ods"  -> "application/vnd.oasis.opendocument.spreadsheet";
            case "rtf"  -> "application/rtf";
            case "txt"  -> "text/plain";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png"  -> "image/png";
            case "webp" -> "image/webp";
            case "heic" -> "image/heic";
            default     -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"garantsia-" + order.getOrderNumber() + "." + extension + "\"")
                .body(fileBytes);
    }

    /**
     * Отказва поръчка
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDTO> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam String reason) {

        User currentUser = securityHelper.getCurrentUser();
        OrderResponseDTO order = orderService.getOrderById(orderId);

        if (!currentUser.isAdmin() && !ownsOrder(currentUser, order)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        OrderResponseDTO cancelledOrder = orderService.cancelOrder(orderId, reason);
        return ResponseEntity.ok(cancelledOrder);
    }

    /**
     * Checks whether the given user owns the order.
     * Preferred: compare by userId (registered users).
     * Fallback: compare by email (guest orders where userId is null).
     */
    private boolean ownsOrder(User currentUser, OrderResponseDTO order) {
        if (order.getUserId() != null) {
            return order.getUserId().equals(currentUser.getId());
        }
        return order.getCustomerEmail() != null
                && order.getCustomerEmail().equals(currentUser.getEmail());
    }
}
