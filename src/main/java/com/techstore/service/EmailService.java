package com.techstore.service;

import com.techstore.entity.Order;
import com.techstore.enums.OrderStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending email notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:CareTech}")
    private String appName;

    @Value("${app.url:http://63.180.10.154:3000}")
    private String appUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Send order confirmation email
     */
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        try {
            log.info("Sending order confirmation email to: {}", order.getCustomerEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("orderDate", order.getCreatedAt().format(DATE_FORMATTER));
            variables.put("order", order);
            variables.put("items", order.getOrderItems());
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("orderUrl", appUrl + "/orders/" + order.getOrderNumber());

            String subject = appName + " - Потвърждение на поръчка #" + order.getOrderNumber();
            sendHtmlEmail(order.getCustomerEmail(), subject, "order-confirmation", variables);

            log.info("Order confirmation email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send order status update email
     */
    @Async
    public void sendOrderStatusUpdateEmail(Order order, OrderStatus previousStatus) {
        try {
            log.info("Sending order status update email to: {} (status changed from {} to {})",
                    order.getCustomerEmail(), previousStatus, order.getStatus());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("previousStatus", getStatusInBulgarian(previousStatus));
            variables.put("newStatus", getStatusInBulgarian(order.getStatus()));
            variables.put("order", order);
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("orderUrl", appUrl + "/orders/" + order.getOrderNumber());
            variables.put("trackingNumber", order.getTrackingNumber());

            String subject = appName + " - Промяна в статуса на поръчка #" + order.getOrderNumber();
            sendHtmlEmail(order.getCustomerEmail(), subject, "order-status-update", variables);

            log.info("Order status update email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order status update email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send order shipped email with tracking information
     */
    @Async
    public void sendOrderShippedEmail(Order order) {
        try {
            log.info("Sending order shipped email to: {}", order.getCustomerEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("trackingNumber", order.getTrackingNumber());
            variables.put("shippingMethod", order.getShippingMethod());
            variables.put("order", order);
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("orderUrl", appUrl + "/orders/" + order.getOrderNumber());

            String subject = appName + " - Вашата поръчка е изпратена #" + order.getOrderNumber();
            sendHtmlEmail(order.getCustomerEmail(), subject, "order-shipped", variables);

            log.info("Order shipped email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order shipped email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send order delivered email
     */
    @Async
    public void sendOrderDeliveredEmail(Order order) {
        try {
            log.info("Sending order delivered email to: {}", order.getCustomerEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("order", order);
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("orderUrl", appUrl + "/orders/" + order.getOrderNumber());

            String subject = appName + " - Вашата поръчка е доставена #" + order.getOrderNumber();
            sendHtmlEmail(order.getCustomerEmail(), subject, "order-delivered", variables);

            log.info("Order delivered email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order delivered email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send order cancelled email
     */
    @Async
    public void sendOrderCancelledEmail(Order order) {
        try {
            log.info("Sending order cancelled email to: {}", order.getCustomerEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("order", order);
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);

            String subject = appName + " - Вашата поръчка е отменена #" + order.getOrderNumber();
            sendHtmlEmail(order.getCustomerEmail(), subject, "order-cancelled", variables);

            log.info("Order cancelled email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancelled email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send HTML email using Thymeleaf template
     */
    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process("email/" + templateName, context);

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Get order status in Bulgarian
     */
    private String getStatusInBulgarian(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Изчакваща";
            case CONFIRMED -> "Потвърдена";
            case PROCESSING -> "В обработка";
            case SHIPPED -> "Изпратена";
            case DELIVERED -> "Доставена";
            case CANCELLED -> "Отменена";
            case REFUNDED -> "Възстановена";
        };
    }
}