package com.techstore.repository;

import com.techstore.entity.Manufacturer;
import com.techstore.entity.Product;
import com.techstore.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("AdminProductSpecifications — админ търсене на продукти")
class AdminProductSpecificationsTest {

    @Autowired TestEntityManager entityManager;
    @Autowired ProductRepository productRepository;

    private Product hiddenImouCamera;
    private Product memoryCard;
    private Product tapoCamera;

    private Manufacturer manufacturer(String name) {
        Manufacturer m = new Manufacturer();
        m.setName(name);
        return entityManager.persist(m);
    }

    private Product product(String nameBg, String sku, Manufacturer manufacturer) {
        Product p = new Product();
        p.setNameBg(nameBg);
        p.setSku(sku);
        p.setManufacturer(manufacturer);
        p.setStatus(ProductStatus.AVAILABLE);
        return p;
    }

    private List<String> search(String query, boolean onSale, boolean withMarkup) {
        return productRepository.findAll(
                        AdminProductSpecifications.adminSearch(query, onSale, withMarkup),
                        PageRequest.of(0, 20, Sort.by("nameBg")))
                .map(Product::getNameBg)
                .getContent();
    }

    @BeforeEach
    void setUp() {
        Manufacturer imou = manufacturer("IMOU");
        Manufacturer kingston = manufacturer("Kingston");

        hiddenImouCamera = product("Камера Ranger 2", "IPC-A22EP", imou);
        hiddenImouCamera.setShow(false);
        hiddenImouCamera.setStatus(ProductStatus.NOT_AVAILABLE);
        hiddenImouCamera.setDiscount(new BigDecimal("10"));
        entityManager.persist(hiddenImouCamera);

        memoryCard = product("MicroSD карта 128GB", "SDCS2/128GB", kingston);
        memoryCard.setMarkupPercentage(new BigDecimal("25"));
        entityManager.persist(memoryCard);

        tapoCamera = product("Камера Tapo C200", "TAPO-C200", null);
        tapoCamera.setBarcode("6935364088435");
        entityManager.persist(tapoCamera);

        Product deleted = product("Камера IMOU стар модел", "IPC-OLD", imou);
        deleted.setDeleted(true);
        entityManager.persist(deleted);

        entityManager.flush();
    }

    @Test
    @DisplayName("Всяка дума трябва да съвпадне — име и марка; скритите се намират, изтритите не")
    void everyWordMustMatchAcrossFields() {
        assertThat(search("imou камера", false, false)).containsExactly("Камера Ranger 2");
        assertThat(search("камера", false, false)).containsExactly("Камера Ranger 2", "Камера Tapo C200");
    }

    @Test
    @DisplayName("Кирилица се търси и с транслитерация")
    void cyrillicWordMatchesItsLatinForm() {
        assertThat(search("имоу", false, false)).containsExactly("Камера Ranger 2");
    }

    @Test
    @DisplayName("Част от SKU, без значение от главни/малки букви")
    void partialSkuIsCaseInsensitive() {
        assertThat(search("sdcs2", false, false)).containsExactly("MicroSD карта 128GB");
    }

    @Test
    @DisplayName("Точен баркод и ID")
    void exactBarcodeAndId() {
        assertThat(search("6935364088435", false, false)).containsExactly("Камера Tapo C200");
        assertThat(search(String.valueOf(memoryCard.getId()), false, false)).contains("MicroSD карта 128GB");
    }

    @Test
    @DisplayName("Филтри за промоция и надценка, и без текст")
    void onSaleAndMarkupFilters() {
        assertThat(search(null, true, false)).containsExactly("Камера Ranger 2");
        assertThat(search("  ", false, true)).containsExactly("MicroSD карта 128GB");
        assertThat(search("tapo", true, false)).isEmpty();
    }

    @Test
    @DisplayName("% и _ се търсят буквално")
    void likeWildcardsAreEscaped() {
        assertThat(search("%", false, false)).isEmpty();
        assertThat(search("_", false, false)).isEmpty();
    }
}
