package com.techstore.repository;

import com.techstore.entity.CartItem;
import com.techstore.entity.Product;
import com.techstore.entity.User;
import com.techstore.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("CartItemRepository.countAvailableItemsByUserId — значката в „Клиенти“ брои само наличните")
class CartItemAvailableCountTest {

    @Autowired TestEntityManager em;
    @Autowired CartItemRepository repository;

    @Test
    @DisplayName("Неналичен, скрит, неактивен или изтрит продукт не се брои; бройката е сума от количествата")
    void countsUnitsTheCustomerCanStillBuy() {
        User customer = user("customer@example.com");
        User onlyUnavailable = user("ghost@example.com");

        Product laptop = product("Лаптоп", ProductStatus.AVAILABLE, true, true, false);
        add(customer, laptop, 2);
        add(customer, product("Изчерпан", ProductStatus.NOT_AVAILABLE, false, true, false), 1);
        add(customer, product("Скрит", ProductStatus.AVAILABLE, false, true, false), 1);
        add(customer, product("Неактивен", ProductStatus.AVAILABLE, true, false, false), 1);
        add(customer, product("Изтрит", ProductStatus.AVAILABLE, true, true, true), 1);
        add(onlyUnavailable, product("Ограничен", ProductStatus.LIMITED_QUANTITY, false, true, false), 3);
        em.flush();

        assertThat(repository.countAvailableItemsByUserId(customer.getId())).isEqualTo(2);
        assertThat(repository.countAvailableItemsByUserId(onlyUnavailable.getId())).isNull();
    }

    private User user(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPassword("x");
        u.setRole(User.Role.USER);
        u.setActive(true);
        return em.persist(u);
    }

    private Product product(String name, ProductStatus status, boolean show, boolean active, boolean deleted) {
        Product p = new Product();
        p.setNameBg(name);
        p.setFinalPrice(new BigDecimal("10.00"));
        p.setStatus(status);
        p.setShow(show);
        p.setActive(active);
        p.setDeleted(deleted);
        return em.persist(p);
    }

    private void add(User user, Product product, int quantity) {
        CartItem ci = new CartItem();
        ci.setUser(user);
        ci.setProduct(product);
        ci.setQuantity(quantity);
        em.persist(ci);
    }
}
