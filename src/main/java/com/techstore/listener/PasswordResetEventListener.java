package com.techstore.listener;

import com.techstore.entity.User;
import com.techstore.event.OnPasswordResetRequestEvent;
import com.techstore.repository.UserRepository;
import com.techstore.service.EmailService;
import com.techstore.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for password reset requests
 * Handles sending password reset emails
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PasswordResetEventListener {

    private final EmailService emailService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * Handle password reset request event
     * Sends password reset email to user
     */
    @EventListener
    @Async
    public void handlePasswordResetRequest(OnPasswordResetRequestEvent event) {
        log.info("Processing OnPasswordResetRequestEvent for email: {}", event.getEmail());

        try {
            // Find user by email
            User user = userRepository.findByEmail(event.getEmail().trim().toLowerCase())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate password reset token
            String resetToken = jwtUtil.generatePasswordResetToken(user);

            // Send password reset email
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);

            log.info("Password reset email sent successfully to: {}", event.getEmail());
        } catch (Exception e) {
            log.error("Error handling password reset request event for email: {}", event.getEmail(), e);
        }
    }
}