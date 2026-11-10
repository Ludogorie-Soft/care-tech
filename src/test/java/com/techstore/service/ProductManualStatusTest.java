package com.techstore.service;

import com.techstore.entity.Product;
import com.techstore.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An admin's status wins over the supplier feeds (Product#manualStatus). The admin form sends
 * the status and the visibility checkbox on every save, so only a real change may count as intent.
 */
@DisplayName("Ръчен статус: админът е с приоритет пред доставчика")
class ProductManualStatusTest {

    private static final int AVAILABLE = ProductStatus.AVAILABLE.getCode();
    private static final int NOT_AVAILABLE = ProductStatus.NOT_AVAILABLE.getCode();

    private ProductService service;

    @BeforeEach
    void setUp() {
        // applyAdminStatus / applyAdminVisibility touch only the product.
        service = new ProductService(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static Product supplierProduct(ProductStatus status, boolean show) {
        Product p = new Product();
        p.setStatus(status);
        p.setShow(show);
        return p;
    }

    @Nested
    @DisplayName("Запис от админ панела")
    class AdminSave {

        @Test
        @DisplayName("Смяна на статуса го прави ръчен")
        void changedStatusBecomesManual() {
            Product p = supplierProduct(ProductStatus.NOT_AVAILABLE, false);

            service.applyAdminStatus(p, AVAILABLE, null);

            assertThat(p.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
            assertThat(p.getManualStatus()).isTrue();
        }

        @Test
        @DisplayName("Повторен запис без смяна на статуса не го прави ръчен")
        void resavedStatusStaysWithSupplier() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);

            service.applyAdminStatus(p, AVAILABLE, null);

            assertThat(p.getManualStatus()).isFalse();
        }

        @Test
        @DisplayName("Нов продукт не започва с ръчен статус")
        void newProductIsNotManual() {
            Product p = new Product();

            service.applyAdminStatus(p, AVAILABLE, null);

            assertThat(p.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
            assertThat(p.getManualStatus()).isFalse();
        }

        @Test
        @DisplayName("„Статус от доставчика“ връща статуса на синхронизацията")
        void explicitFalseHandsStatusBack() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);
            p.setManualStatus(true);

            service.applyAdminStatus(p, AVAILABLE, false);

            assertThat(p.getManualStatus()).isFalse();
        }

        @Test
        @DisplayName("Изрично ръчен статус остава ръчен и без смяна")
        void explicitTrueKeepsManual() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);

            service.applyAdminStatus(p, AVAILABLE, true);

            assertThat(p.getManualStatus()).isTrue();
        }

        @Test
        @DisplayName("Скрит от sync-а продукт не става ръчно скрит при запис с непипната отметка")
        void untouchedCheckboxDoesNotHide() {
            // The sync hid it for lack of stock; the form loads show=false and sends it back.
            Product p = supplierProduct(ProductStatus.NOT_AVAILABLE, false);

            service.applyAdminStatus(p, AVAILABLE, null);
            service.applyAdminVisibility(p, false);

            assertThat(p.getManuallyHidden()).isFalse();
            assertThat(p.getManualStatus()).isTrue();
        }

        @Test
        @DisplayName("Махната отметка скрива ръчно; поставена отново — отменя")
        void changedCheckboxIsIntent() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);

            service.applyAdminVisibility(p, false);
            assertThat(p.getShow()).isFalse();
            assertThat(p.getManuallyHidden()).isTrue();

            service.applyAdminVisibility(p, true);
            assertThat(p.getShow()).isTrue();
            assertThat(p.getManuallyHidden()).isFalse();
        }

        @Test
        @DisplayName("Ръчно скрит продукт остава скрит при повторен запис")
        void manuallyHiddenSurvivesResave() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, false);
            p.setManuallyHidden(true);

            service.applyAdminVisibility(p, false);

            assertThat(p.getManuallyHidden()).isTrue();
        }
    }

    @Nested
    @DisplayName("Синхронизация (Product#applyFeedStatus)")
    class FeedStatus {

        @Test
        @DisplayName("Без ръчен статус доставчикът задава статуса")
        void feedSetsStatus() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);

            assertThat(p.applyFeedStatus(ProductStatus.NOT_AVAILABLE)).isEqualTo(ProductStatus.NOT_AVAILABLE);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Ръчен „Наличен“ остава, когато доставчикът казва „Неналичен“")
        void manualAvailableWins() {
            Product p = supplierProduct(ProductStatus.AVAILABLE, true);
            p.setManualStatus(true);

            assertThat(p.applyFeedStatus(ProductStatus.NOT_AVAILABLE)).isEqualTo(ProductStatus.AVAILABLE);
            assertThat(p.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
        }

        @Test
        @DisplayName("Ръчен „Неналичен“ остава, когато доставчикът казва „Наличен“")
        void manualNotAvailableWins() {
            Product p = supplierProduct(ProductStatus.NOT_AVAILABLE, false);
            p.setManualStatus(true);

            assertThat(p.applyFeedStatus(ProductStatus.AVAILABLE)).isEqualTo(ProductStatus.NOT_AVAILABLE);
        }
    }

    @Test
    @DisplayName("Кодовете на статуса съвпадат с формата в админа")
    void statusCodes() {
        assertThat(NOT_AVAILABLE).isZero();
        assertThat(AVAILABLE).isEqualTo(1);
    }
}
