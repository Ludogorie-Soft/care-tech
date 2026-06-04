package com.techstore.service;

import com.techstore.dto.request.MessageToAdmin;
import com.techstore.entity.CartItem;
import com.techstore.entity.Order;
import com.techstore.entity.PersonalOffer;
import com.techstore.entity.User;
import com.techstore.enums.OrderStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
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
    private final S3Service s3Service;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:CareTech}")
    private String appName;

    @Value("${app.url:https://www.caretech.bg}")
    private String appUrl;

    @Value("${app.email.info:info@caretech.bg}")
    private String infoEmail;

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
            variables.put("orderUrl", appUrl + "/orders/" + order.getId());

            String subject = appName + " - Потвърждение на поръчка #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "order-confirmation", variables);

            log.info("Order confirmation email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send email to admin
     */
    @Async
    public void sendNewOrderNotificationToAdmin(Order order) {
        try {
            log.info("Sending admin order notification email to: {}", fromEmail);

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("orderDate", order.getCreatedAt().format(DATE_FORMATTER));
            variables.put("order", order);
            variables.put("items", order.getOrderItems());
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("orderUrl", appUrl + "/admin/dashboard/orders/" + order.getId());

            String subject = appName + " - Нова поръчка #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, fromEmail, subject, "admin-new-order", variables);

            log.info("Admin order notification email sent successfully to: {}", fromEmail);
        } catch (Exception e) {
            log.error("Failed to send admin order notification email", e);
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
            variables.put("orderUrl", appUrl + "/orders/" + order.getId());
            variables.put("trackingNumber", order.getTrackingNumber());

            String subject = appName + " - Промяна в статуса на поръчка #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "order-status-update", variables);

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
            variables.put("orderUrl", appUrl + "/orders/" + order.getId());

            String subject = appName + " - Вашата поръчка е изпратена #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "order-shipped", variables, order.getWarrantyFilePath());

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
            variables.put("orderUrl", appUrl + "/orders/" + order.getId());

            String subject = appName + " - Вашата поръчка е доставена #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "order-delivered", variables);

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
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "order-cancelled", variables);

            log.info("Order cancelled email sent successfully to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancelled email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send leasing rejected email to customer
     */
    @Async
    public void sendLeasingRejectedEmail(Order order, String productName) {
        try {
            log.info("Sending leasing rejected email to: {}", order.getCustomerEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", order.getCustomerFirstName() + " " + order.getCustomerLastName());
            variables.put("orderNumber", order.getOrderNumber());
            variables.put("productName", productName);
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);

            String subject = appName + " - Заявката за лизинг не беше одобрена #" + order.getOrderNumber();
            sendHtmlEmail(fromEmail, order.getCustomerEmail(), subject, "leasing-rejected", variables);

            log.info("Leasing rejected email sent to: {}", order.getCustomerEmail());
        } catch (Exception e) {
            log.error("Failed to send leasing rejected email to: {}", order.getCustomerEmail(), e);
        }
    }

    /**
     * Send password reset email
     */
    @Async
    public void sendPasswordResetEmail(String email, String resetToken) {
        try {
            log.info("Sending password reset email to: {}", email);

            // Create reset URL
            String resetUrl = appUrl + "/reset-password?token=" + resetToken;

            Map<String, Object> variables = new HashMap<>();
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("customerName", email);
            variables.put("resetUrl", resetUrl);
            variables.put("resetToken", resetToken);
            variables.put("validUntil", "1 час");

            String subject = appName + " - Нулиране на парола";
            sendHtmlEmail(infoEmail, email, subject, "password-reset-email", variables);

            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
        }
    }

    /**
     * Send password changed confirmation email
     */
    @Async
    public void sendPasswordChangedEmail(String email, String username) {
        try {
            log.info("Sending password changed confirmation email to: {}", email);

            Map<String, Object> variables = new HashMap<>();
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);
            variables.put("customerName", username);
            variables.put("changeTime", LocalDateTime.now().format(DATE_FORMATTER));

            String subject = appName + " - Паролата е променена успешно";
            sendHtmlEmail(infoEmail, email, subject, "password-changed-confirmation", variables);

            log.info("Password changed confirmation email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password changed confirmation email to: {}", email, e);
        }
    }

    @Async
    public void sendMessageToAdmin(MessageToAdmin dto) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");

            Context ctx = new Context();
            ctx.setVariable("name", dto.getName());
            ctx.setVariable("email", dto.getEmail());
            ctx.setVariable("phone", dto.getPhone());
            ctx.setVariable("message", dto.getMessage());
            ctx.setVariable("appName", appName);

            String html = templateEngine.process("email/message-to-admin", ctx);

            helper.setFrom(infoEmail);
            helper.setReplyTo(dto.getEmail());
            helper.setTo(infoEmail);
            helper.setSubject("Ново съобщение от " + dto.getName());
            helper.setText(html, true);
            mime.addHeader("Sender", fromEmail);


            mailSender.send(mime);

            log.info("Admin message sent from {} <{}>", dto.getName(), dto.getEmail());

        } catch (Exception e) {
            log.error("Failed to send admin message: {}", dto, e);
        }
    }

    /**
     * Send abandoned cart reminder email
     */
    @Async
    public void sendAbandonedCartEmail(User user, List<CartItem> items) {
        try {
            log.info("Sending abandoned cart reminder to: {}", user.getEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", user.getFirstName() != null ? user.getFirstName() : user.getEmail());
            variables.put("cartItems", items);
            variables.put("cartUrl", appUrl + "/cart");
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);

            String subject = appName + " - Забравихте нещо в количката си 🛒";
            sendHtmlEmail(fromEmail, user.getEmail(), subject, "abandoned-cart", variables);

            log.info("Abandoned cart reminder sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send abandoned cart reminder to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send personal offer email to a user
     */
    @Async
    public void sendPersonalOfferEmail(User user, PersonalOffer offer) {
        try {
            log.info("Sending personal offer email to: {}", user.getEmail());

            Map<String, Object> variables = new HashMap<>();
            variables.put("customerName", user.getFirstName() + " " + user.getLastName());
            variables.put("offerTitle", offer.getTitle());
            variables.put("offerMessage", offer.getMessage());
            variables.put("offerItems", offer.getOfferItems());
            variables.put("discountPercent", offer.getDiscountPercent());
            variables.put("expiresAt", offer.getExpiresAt() != null
                    ? offer.getExpiresAt().format(DATE_FORMATTER) : null);
            variables.put("offersUrl", appUrl + "/profile/my-offers");
            variables.put("appName", appName);
            variables.put("appUrl", appUrl);

            String subject = appName + " - Лична оферта за вас: " + offer.getTitle();
            sendHtmlEmail(fromEmail, user.getEmail(), subject, "personal-offer", variables);

            log.info("Personal offer email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send personal offer email to: {}", user.getEmail(), e);
        }
    }

    /**
     * Send a plain HTML email without a Thymeleaf template.
     * Used for internal admin notifications (e.g. TBI leasing status updates).
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mime);
            log.info("HTML email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send HTML email using Thymeleaf template
     */
    private void sendHtmlEmail(String from, String to, String subject, String templateName, Map<String, Object> variables)
            throws MessagingException {
        sendHtmlEmail(from, to, subject, templateName, variables, null);
    }

    /**
     * Send HTML email with optional file attachment
     */
    private void sendHtmlEmail(String from, String to, String subject, String templateName,
                                Map<String, Object> variables, String attachmentPath)
            throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process("email/" + templateName, context);

        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        // When sending from a different address than the SMTP account,
        // add Sender header (RFC 5321) so spam filters don't flag the mismatch
        if (!from.equals(fromEmail)) {
            message.addHeader("Sender", fromEmail);
        }

        if (attachmentPath != null) {
            try {
                byte[] fileBytes = s3Service.downloadFileBytes(attachmentPath);
                String filename = attachmentPath.substring(attachmentPath.lastIndexOf('/') + 1);
                helper.addAttachment(filename, new ByteArrayResource(fileBytes));
            } catch (Exception e) {
                log.warn("Could not attach warranty file from S3, skipping: {}", attachmentPath, e);
            }
        }

        mailSender.send(message);
    }

    /**
     * Get order status in Bulgarian
     */
    private String getStatusInBulgarian(OrderStatus status) {
        return switch (status) {
            case LEASING_PENDING -> "Чака одобрение от TBI";
            case LEASING_REJECTED -> "Лизинг отказан";
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