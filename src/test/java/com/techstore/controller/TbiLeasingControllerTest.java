package com.techstore.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.config.SecurityConfig;
import com.techstore.dto.tbi.LeasingApplicationResponseDto;
import com.techstore.dto.tbi.TbiRegisterRequestDto;
import com.techstore.dto.tbi.TbiRegisterResponseDto;
import com.techstore.dto.tbi.TbiStatusWebhookDto;
import com.techstore.entity.User;
import com.techstore.repository.UserRepository;
import com.techstore.service.TbiLeasingService;
import com.techstore.util.JwtUtil;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TbiLeasingController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.cors.allowedOrigins=http://localhost:3000")
@DisplayName("TbiLeasingController — security и авторизация")
class TbiLeasingControllerTest {

    @Autowired MockMvc       mockMvc;
    @Autowired ObjectMapper  objectMapper;

    @MockBean TbiLeasingService tbiLeasingService;
    @MockBean SecurityHelper    securityHelper;
    @MockBean UserRepository    userRepository;
    @MockBean JwtUtil           jwtUtil;

    // ── helpers ───────────────────────────────────────────────────────────────

    private User userWithEmail(String email, User.Role role) {
        User u = new User();
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    // ── POST /api/tbi/register ────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/tbi/register")
    class Register {

        @Test
        @DisplayName("Без автентикация → 401 Unauthorized")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/tbi/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Автентикиран потребител → 200 OK")
        void authenticated_returns200() throws Exception {
            User user = userWithEmail("user@test.com", User.Role.USER);
            when(securityHelper.getCurrentUser()).thenReturn(user);
            when(tbiLeasingService.registerApplication(any(), any()))
                    .thenReturn(new TbiRegisterResponseDto(1L, "https://tbi.example.com/iframe"));

            mockMvc.perform(post("/api/tbi/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new TbiRegisterRequestDto())))
                    .andExpect(status().isOk());
        }
    }

    // ── POST /api/tbi/webhook ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/tbi/webhook")
    class Webhook {

        @Test
        @DisplayName("Без автентикация → 200 OK (публичен endpoint)")
        void noAuth_returns200() throws Exception {
            TbiStatusWebhookDto payload = new TbiStatusWebhookDto();
            payload.setResellerCode("BIVD");
            payload.setOrderId("12345");
            payload.setMessage("ContractSigned");

            doNothing().when(tbiLeasingService).processStatusWebhook(any());

            mockMvc.perform(post("/api/tbi/webhook")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }
    }

    // ── GET /api/tbi/application/{id} ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/tbi/application/{id} — IDOR защита")
    class GetApplicationStatus {

        @Test
        @DisplayName("Без автентикация → 401 Unauthorized")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/tbi/application/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Собственикът на заявката → 200 OK")
        void owner_returns200() throws Exception {
            User owner = userWithEmail("owner@test.com", User.Role.USER);
            LeasingApplicationResponseDto dto = new LeasingApplicationResponseDto();
            dto.setCustomerEmail("owner@test.com");

            when(securityHelper.getCurrentUser()).thenReturn(owner);
            when(tbiLeasingService.getApplicationStatus(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/tbi/application/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Друг потребител (не е собственик) → 403 Forbidden")
        void nonOwner_returns403() throws Exception {
            User nonOwner = userWithEmail("other@test.com", User.Role.USER);
            LeasingApplicationResponseDto dto = new LeasingApplicationResponseDto();
            dto.setCustomerEmail("owner@test.com");

            when(securityHelper.getCurrentUser()).thenReturn(nonOwner);
            when(tbiLeasingService.getApplicationStatus(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/tbi/application/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Администратор може да вижда всяка заявка → 200 OK")
        void admin_returns200() throws Exception {
            User admin = userWithEmail("admin@test.com", User.Role.ADMIN);
            LeasingApplicationResponseDto dto = new LeasingApplicationResponseDto();
            dto.setCustomerEmail("someone@test.com");

            when(securityHelper.getCurrentUser()).thenReturn(admin);
            when(tbiLeasingService.getApplicationStatus(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/tbi/application/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("SUPER_ADMIN може да вижда всяка заявка → 200 OK")
        void superAdmin_returns200() throws Exception {
            User superAdmin = userWithEmail("superadmin@test.com", User.Role.SUPER_ADMIN);
            LeasingApplicationResponseDto dto = new LeasingApplicationResponseDto();
            dto.setCustomerEmail("someone@test.com");

            when(securityHelper.getCurrentUser()).thenReturn(superAdmin);
            when(tbiLeasingService.getApplicationStatus(1L)).thenReturn(dto);

            mockMvc.perform(get("/api/tbi/application/1"))
                    .andExpect(status().isOk());
        }
    }
}
