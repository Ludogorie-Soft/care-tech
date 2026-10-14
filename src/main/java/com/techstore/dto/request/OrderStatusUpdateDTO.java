package com.techstore.dto.request;

import com.techstore.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderStatusUpdateDTO {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    /** Blank clears the stored tracking number; null leaves it unchanged. */
    @Size(max = 100)
    private String trackingNumber;

    /**
     * Shown to the customer in the status email. Blank clears the stored note; null leaves it unchanged.
     * Same limit as {@link OrderUpdateRequestDTO#getAdminNotes()}, since both modals edit the same field.
     */
    @Size(max = 2000)
    private String adminNotes;

    /** Whether to email the customer about this change. Null means yes, so older clients keep working. */
    private Boolean notifyCustomer;
}
