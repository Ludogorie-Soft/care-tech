package com.techstore.controller;

import com.techstore.config.SecurityConfig;
import com.techstore.dto.filter.FilterConfigDto.AttributeSummary;
import com.techstore.exception.ValidationException;
import com.techstore.repository.UserRepository;
import com.techstore.service.filter.FilterConfigService;
import com.techstore.util.JwtUtil;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilterConfigController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.cors.allowedOrigins=http://localhost:3000")
@DisplayName("FilterConfigController — само за админи")
class FilterConfigControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean FilterConfigService service;
    @MockBean SecurityHelper securityHelper;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;

    private static AttributeSummary colour() {
        return new AttributeSummary(1L, "colour", "Цвят", "Colour", "ENUM", null, null, "MANUAL", false, 40, 18, 5000);
    }

    @Test
    @DisplayName("Без автентикация → 401")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/filters/attributes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Обикновен потребител → 403")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/filters/attributes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Админ → 200 със свойствата")
    void adminListsAttributes() throws Exception {
        when(service.listAttributes(any())).thenReturn(List.of(colour()));
        mockMvc.perform(get("/api/admin/filters/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("colour"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Невалиден регулярен израз → 400 със съобщението")
    void invalidPatternIsABadRequest() throws Exception {
        when(service.createValueRule(eq(1L), any()))
                .thenThrow(new ValidationException("Невалиден регулярен израз: parentheses () not balanced"));
        mockMvc.perform(post("/api/admin/filters/attributes/1/value-rules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pattern\":\"(a\",\"valueId\":5}"))
                .andExpect(status().isBadRequest());
    }
}
