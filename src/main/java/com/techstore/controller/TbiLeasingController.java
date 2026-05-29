package com.techstore.controller;

import com.techstore.dto.tbi.LeasingApplicationResponseDto;
import com.techstore.dto.tbi.TbiRegisterRequestDto;
import com.techstore.dto.tbi.TbiRegisterResponseDto;
import com.techstore.dto.tbi.TbiStatusWebhookDto;
import com.techstore.entity.User;
import com.techstore.service.TbiLeasingService;
import com.techstore.util.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tbi")
@RequiredArgsConstructor
@Slf4j
public class TbiLeasingController {

    private final TbiLeasingService tbiLeasingService;
    private final SecurityHelper    securityHelper;

    /**
     * POST /api/tbi/register
     *
     * Called by the frontend when the customer clicks "Buy with TBI".
     * Encrypts product data, registers the application with TBI Bank, and returns
     * the iframe URL to display to the customer.
     *
     * Requires authentication (ADMIN-only during testing phase).
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/register")
    public ResponseEntity<TbiRegisterResponseDto> registerApplication(
            @RequestBody TbiRegisterRequestDto request) {

        User currentUser = securityHelper.getCurrentUser();
        TbiRegisterResponseDto response = tbiLeasingService.registerApplication(request, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/tbi/webhook
     *
     * PUBLIC endpoint — called by TBI Bank when an application changes state.
     * No JWT required; TBI identifies itself via ResellerCode in the payload.
     *
     * Always returns 200 OK — TBI expects a 2xx to stop retrying.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> statusWebhook(
            @RequestBody TbiStatusWebhookDto payload) {

        log.info("TBI webhook: orderId={} status={} creditAppId={}",
                payload.getOrderId(),
                payload.resolvedStatus(),
                payload.getCreditApplicationId());

        tbiLeasingService.processStatusWebhook(payload);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/tbi/application/{id}
     *
     * Returns the current status of a leasing application from our DB.
     * Optionally polls TBI for the latest status if the webhook hasn't arrived yet.
     *
     * Requires authentication.
     */
    @GetMapping("/application/{id}")
    public ResponseEntity<LeasingApplicationResponseDto> getApplicationStatus(
            @PathVariable Long id) {

        LeasingApplicationResponseDto response = tbiLeasingService.getApplicationStatus(id);

        User currentUser = securityHelper.getCurrentUser();
        if (!currentUser.isAdmin()
                && !currentUser.getEmail().equalsIgnoreCase(response.getCustomerEmail())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(response);
    }
}
