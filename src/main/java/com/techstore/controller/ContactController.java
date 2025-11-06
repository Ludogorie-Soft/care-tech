package com.techstore.controller;

import com.techstore.dto.request.MessageToAdmin;
import com.techstore.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/contact")
public class ContactController {

    private final EmailService emailService;

    @PostMapping("/contact-us")
    public ResponseEntity<String> contactUs(@Valid @RequestBody MessageToAdmin messageToAdmin) {
        try {
            emailService.sendMessageToAdmin(messageToAdmin);
            return ResponseEntity.ok("Message sent successfully!");
        } catch (Exception e) {
            log.error("Failed to send message to admin");
            return ResponseEntity.badRequest().body("Failed to send message to admin");
        }
    }
}
