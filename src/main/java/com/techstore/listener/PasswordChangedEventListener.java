package com.techstore.listener;

import com.techstore.entity.User;
import com.techstore.event.OnPasswordChangedEvent;
import com.techstore.repository.UserRepository;
import com.techstore.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for password changed events
 * Handles sending password changed confirmation emails
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordChangedEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Handle password changed event
     * Sends confirmation email to user
     */
    @EventListener
    @Async
    public void handlePasswordChanged(OnPasswordChangedEvent event) {
        log.info("Processing OnPasswordChangedEvent for email: {}", event.getEmail());

        try {
            // Find user by email
            User user = userRepository.findByEmail(event.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Send password changed confirmation email
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());

            log.info("Password changed confirmation email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error handling password changed event for email: {}", event.getEmail(), e);
        }
    }
}