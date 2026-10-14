package com.techstore.service;

import com.techstore.entity.CartItem;
import com.techstore.entity.User;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.cart.abandoned-reminder-after-days:3}")
    private int reminderAfterDays;

    @Value("${app.cart.abandoned-reminder-resend-after-days:30}")
    private int resendAfterDays;

    @Scheduled(cron = "0 0 9 * * ?") // Every day at 09:00
    @Transactional
    public void sendAbandonedCartReminders() {
        LocalDateTime activityCutoff = LocalDateTime.now().minusDays(reminderAfterDays);
        LocalDateTime resendCutoff = LocalDateTime.now().minusDays(resendAfterDays);

        List<User> users = cartItemRepository.findUsersWithAbandonedCarts(activityCutoff, resendCutoff);

        if (users.isEmpty()) {
            log.info("Abandoned cart job: no eligible users found");
            return;
        }

        log.info("Abandoned cart job: sending reminders to {} users", users.size());

        for (User user : users) {
            try {
                List<CartItem> items = cartItemRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
                if (items.isEmpty()) continue;

                emailService.sendAbandonedCartEmail(user, items);

                user.setAbandonedCartReminderSentAt(LocalDateTime.now());
                userRepository.save(user);

                log.info("Abandoned cart reminder sent to user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to process abandoned cart reminder for user {}", user.getId(), e);
            }
        }

        log.info("Abandoned cart job finished. Processed {} users", users.size());
    }
}
