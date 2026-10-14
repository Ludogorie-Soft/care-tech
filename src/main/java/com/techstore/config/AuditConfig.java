package com.techstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.annotation.PostConstruct;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.TimeZone;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditAwareImpl", dateTimeProviderRef = "dateTimeProvider")
public class AuditConfig {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Sofia"));
    }

    @Bean
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneId.of("Europe/Sofia")));
    }

    @Bean(name = "auditAwareImpl")
    public AuditorAware<String> auditAwareImpl() {
        return new AuditorAwareImpl();
    }

    public static class AuditorAwareImpl implements AuditorAware<String> {
        @Override
        public Optional<String> getCurrentAuditor() {
            try {
                // Пропусни audit за sync операции
                String threadName = Thread.currentThread().getName();
                if (threadName.contains("sync") || threadName.contains("Sync")) {
                    return Optional.of("system");
                }

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    return Optional.of("system");
                }

                Object principal = authentication.getPrincipal();

                if (principal instanceof UserDetails) {
                    return Optional.of(((UserDetails) principal).getUsername());
                } else if (principal instanceof String) {
                    return Optional.of((String) principal);
                }

                return Optional.of("system");

            } catch (Exception e) {
                return Optional.of("system");
            }
        }
    }
}