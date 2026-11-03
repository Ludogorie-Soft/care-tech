package com.techstore.service;

import com.techstore.dto.response.CartSummaryDto;
import com.techstore.entity.CartItem;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import com.techstore.mapper.ParameterMapper;
import com.techstore.repository.CartItemRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A product that went unavailable stays in cart_items. The admin's customer list counted those rows,
 * so it showed a cart that opened empty; the cart's total and the abandoned-cart reminder priced and
 * listed them too. Only what can still be bought counts; the admin sees the rest apart.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Кошница — броят се само наличните продукти, неналичните са отделно за админа")
class CartSummaryAvailabilityTest {

    private static final long USER_ID = 7L;

    @Mock CartItemRepository cartItemRepository;
    @Mock UserRepository userRepository;
    @Mock ProductRepository productRepository;
    @Mock ParameterMapper parameterMapper;
    @Mock EmailService emailService;

    private CartService cartService;

    private final Product laptop = product(1L, "Лаптоп", "865.83", ProductStatus.AVAILABLE, true);
    private final Product mouse = product(2L, "Мишка", "20.00", ProductStatus.AVAILABLE, true);
    private final Product soldOut = product(3L, "Видеокарта", "1162.95", ProductStatus.NOT_AVAILABLE, false);
    private final Product noPrice = product(4L, "Без цена", null, ProductStatus.NOT_AVAILABLE, false);

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, userRepository, productRepository, parameterMapper);
    }

    @Test
    @DisplayName("Клиентът вижда и плаща само наличните; неналичните не се връщат изобщо")
    void customerCartListsAndPricesAvailableOnly() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .thenReturn(List.of(item(laptop, 1), item(soldOut, 2), item(mouse, 3)));

        CartSummaryDto cart = cartService.getCartSummary(USER_ID, "bg");

        assertThat(cart.getItems()).extracting(i -> i.getProduct().getNameBg()).containsExactly("Лаптоп", "Мишка");
        assertThat(cart.getTotalItems()).isEqualTo(4);
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("925.83");
        assertThat(cart.getUnavailableItems()).isNull();
    }

    @Test
    @DisplayName("Админът вижда неналичните отделно, без да влизат в броя и сумата")
    void adminSeesUnavailableApart() {
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .thenReturn(List.of(item(soldOut, 1), item(noPrice, 1)));

        CartSummaryDto cart = cartService.getCartSummaryForAdmin(USER_ID, "bg");

        assertThat(cart.getItems()).isEmpty();
        assertThat(cart.getTotalItems()).isZero();
        assertThat(cart.getTotalPrice()).isEqualByComparingTo("0");
        assertThat(cart.getUnavailableItems()).extracting(i -> i.getProduct().getNameBg())
                .containsExactly("Видеокарта", "Без цена");
        assertThat(cart.getUnavailableItems().get(1).getItemTotal()).isNull();
    }

    @Test
    @DisplayName("Напомняне за изоставена кошница — само с наличните; без наличен продукт няма имейл")
    void abandonedCartReminderSkipsUnavailable() {
        AbandonedCartService service = new AbandonedCartService(cartItemRepository, userRepository, emailService);
        ReflectionTestUtils.setField(service, "reminderAfterDays", 3);
        ReflectionTestUtils.setField(service, "resendAfterDays", 30);
        User withBoth = user(USER_ID);
        User onlySoldOut = user(8L);
        when(cartItemRepository.findUsersWithAbandonedCarts(any(), any())).thenReturn(List.of(withBoth, onlySoldOut));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(USER_ID))
                .thenReturn(List.of(item(laptop, 1), item(soldOut, 1)));
        when(cartItemRepository.findByUserIdOrderByCreatedAtAsc(8L)).thenReturn(List.of(item(soldOut, 1)));

        service.sendAbandonedCartReminders();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartItem>> items = ArgumentCaptor.forClass(List.class);
        verify(emailService).sendAbandonedCartEmail(org.mockito.ArgumentMatchers.eq(withBoth), items.capture());
        assertThat(items.getValue()).extracting(ci -> ci.getProduct().getNameBg()).containsExactly("Лаптоп");
        verify(emailService, never()).sendAbandonedCartEmail(org.mockito.ArgumentMatchers.eq(onlySoldOut), anyList());
    }

    private static Product product(long id, String name, String price, ProductStatus status, boolean show) {
        Product p = new Product();
        p.setId(id);
        p.setNameBg(name);
        p.setFinalPrice(price == null ? null : new BigDecimal(price));
        p.setStatus(status);
        p.setActive(true);
        p.setShow(show);
        p.setDeleted(false);
        return p;
    }

    private static CartItem item(Product product, int quantity) {
        CartItem ci = new CartItem();
        ci.setProduct(product);
        ci.setQuantity(quantity);
        return ci;
    }

    private static User user(long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("u" + id + "@example.com");
        return u;
    }
}
