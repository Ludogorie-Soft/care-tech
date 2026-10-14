package com.techstore.controller;

import com.techstore.config.SecurityConfig;
import com.techstore.dto.request.OrderStatusUpdateDTO;
import com.techstore.dto.response.OrderResponseDTO;
import com.techstore.repository.SyncLogRepository;
import com.techstore.repository.UserRepository;
import com.techstore.service.admin.AdminService;
import com.techstore.service.CartService;
import com.techstore.service.CategoryService;
import com.techstore.service.ImageMigrationService;
import com.techstore.service.ManufacturerService;
import com.techstore.service.OrderService;
import com.techstore.service.ParameterService;
import com.techstore.service.PersonalOfferService;
import com.techstore.service.ProductService;
import com.techstore.service.TbiLeasingService;
import com.techstore.util.JwtUtil;
import com.techstore.util.SecurityHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "spring.cors.allowedOrigins=http://localhost:3000")
@DisplayName("AdminController — поръчка за админ страницата и смяна на статус")
class AdminOrderControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean AdminService adminService;
    @MockBean OrderService orderService;
    @MockBean ProductService productService;
    @MockBean ParameterService parameterService;
    @MockBean CategoryService categoryService;
    @MockBean ManufacturerService manufacturerService;
    @MockBean SyncLogRepository syncLogRepository;
    @MockBean TbiLeasingService tbiLeasingService;
    @MockBean CartService cartService;
    @MockBean PersonalOfferService personalOfferService;
    @MockBean ImageMigrationService imageMigrationService;
    @MockBean SecurityHelper securityHelper;
    @MockBean UserRepository userRepository;
    @MockBean JwtUtil jwtUtil;

    @Test
    @DisplayName("Без автентикация → 401")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/orders/5")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Клиент → 403 (бележката е само за админа)")
    void customerIsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/orders/5")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Админ → поръчката заедно с админ бележката")
    void adminGetsTheOrderWithItsNote() throws Exception {
        when(orderService.getOrderById(5L)).thenReturn(OrderResponseDTO.builder()
                .id(5L).orderNumber("ORD-2026-00005").adminNotes("Няма наличност.").build());

        mockMvc.perform(get("/api/admin/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-00005"))
                .andExpect(jsonPath("$.adminNotes").value("Няма наличност."));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Литералните пътища (/orders/latest) не се бъркат с /orders/{id}")
    void literalOrderRoutesStillWin() throws Exception {
        mockMvc.perform(get("/api/admin/orders/latest")).andExpect(status().isOk());
        verify(orderService).getLatestOrders(anyInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Смяна на статус приема notifyCustomer и празна бележка")
    void statusUpdatePassesNotifyCustomerAndBlankNote() throws Exception {
        mockMvc.perform(put("/api/admin/orders/5/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\",\"trackingNumber\":\"\",\"adminNotes\":\"\",\"notifyCustomer\":false}"))
                .andExpect(status().isOk());

        ArgumentCaptor<OrderStatusUpdateDTO> captor = ArgumentCaptor.forClass(OrderStatusUpdateDTO.class);
        verify(orderService).updateOrderStatus(eq(5L), captor.capture());
        assertThat(captor.getValue().getNotifyCustomer()).isFalse();
        assertThat(captor.getValue().getAdminNotes()).isEmpty();
    }
}
