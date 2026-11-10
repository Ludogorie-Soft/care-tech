package com.techstore.service.sync;

import com.techstore.dto.external.NameDto;
import com.techstore.dto.request.ParameterValueRequestDto;
import com.techstore.dto.request.ProductRequestDto;
import com.techstore.entity.Category;
import com.techstore.entity.Manufacturer;
import com.techstore.entity.Parameter;
import com.techstore.entity.ParameterOption;
import com.techstore.entity.Product;
import com.techstore.entity.ProductParameter;
import com.techstore.entity.SyncLog;
import com.techstore.enums.Platform;
import com.techstore.enums.ProductStatus;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.repository.SyncLogRepository;
import com.techstore.service.ValiApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * From 23.10 to 03.11.2026 the VALI products sync saved nothing: Vali moved the reference number
 * "WD-HDD-PASSPORT-6TB-BK" from its item 33385 (now "...-WH") to a new item 38141, our product reached
 * both rows in one chunk, a duplicate product_parameters row was refused, and the run — one transaction —
 * rolled back every price and stock change, its sync log included.
 *
 * <p>Not transactional: each chunk commits in a transaction of its own, which a test transaction
 * around the run would hide.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import({ValiSyncService.class, LogHelper.class, SyncHelper.class})
@DisplayName("ValiSyncService.syncProducts — срещу база: партида по партида")
class ValiProductsSyncDbTest {

    private static final long WD = 130L;
    private static final long COLOUR = 2037L;
    private static final long BLACK = 19098L;
    private static final long WHITE = 19100L;

    @Autowired ValiSyncService service;
    @MockitoSpyBean ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ManufacturerRepository manufacturerRepository;
    @Autowired ParameterRepository parameterRepository;
    @Autowired ParameterOptionRepository parameterOptionRepository;
    @Autowired SyncLogRepository syncLogRepository;
    @Autowired JdbcTemplate jdbc;

    @MockitoBean ValiApiService valiApiService;

    private Manufacturer manufacturer;
    private Category externalDrives;
    private Parameter colour;
    private ParameterOption black;

    @BeforeEach
    void setUp() {
        // PostgreSQL-only SQL (WITH ... UPDATE ... FROM); not what these tests are about.
        doReturn(0).when(productRepository).deduplicateCrossPlatformBySku();

        // In production since 2026-05 (V38); Hibernate does not create it for the test schema.
        jdbc.execute("CREATE UNIQUE INDEX IF NOT EXISTS uq_product_parameters_unique "
                + "ON product_parameters (product_id, parameter_id, parameter_option_id)");

        manufacturer = new Manufacturer();
        manufacturer.setName("WD");
        manufacturer.setExternalId(WD);
        manufacturer = manufacturerRepository.save(manufacturer);

        externalDrives = category("Външни дискове", 490L);

        colour = new Parameter();
        colour.setNameBg("Цвят");
        colour.setExternalId(COLOUR);
        colour.setPlatform(Platform.VALI);
        colour.setCreatedBy("system");
        colour.setLastModifiedBy("system");
        colour = parameterRepository.save(colour);

        black = option("Черен", BLACK);
        option("Бял", WHITE);
    }

    @Test
    @DisplayName("Вали дава кода на продукта на нов запис: продуктът следва своя запис, новият се пропуска")
    void referenceNumberMovedToNewItemIsSkipped() {
        Product blackDrive = existingProduct(33385L, "WD-HDD-PASSPORT-6TB-BK", externalDrives);
        when(valiApiService.getProductsByCategory(490L)).thenReturn(List.of(
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", WHITE),
                item(38141L, "WD-HDD-PASSPORT-6TB-BK", BLACK)));

        service.syncProducts();

        assertThat(colours(blackDrive)).containsExactly("Бял");
        assertThat(productRepository.findByExternalIdIn(List.of(38141L))).isEmpty();
        SyncLog log = lastLog();
        assertThat(log.getStatus()).isEqualTo("SUCCESS");
        assertThat(log.getErrorMessage()).contains("Skipped: 1");
    }

    @Test
    @DisplayName("Партида, която базата отказва, се връща сама — останалото се записва, логът остава")
    void refusedChunkRollsBackAlone() {
        Category gpus = category("Видео карти", 388L);
        Product gpu = existingProduct(37970L, "ASR-VC-B65-CT-32G", gpus);
        Product drive = existingProduct(33385L, "WD-HDD-PASSPORT-6TB-WH", externalDrives);

        when(valiApiService.getProductsByCategory(388L)).thenReturn(List.of(
                item(37970L, "ASR-VC-B65-CT-32G", WHITE)));
        // Two new items with one reference number: the second insert breaks products_reference_number_key.
        when(valiApiService.getProductsByCategory(490L)).thenReturn(List.of(
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", WHITE),
                item(50001L, "DUP-REF", WHITE),
                item(50002L, "DUP-REF", WHITE)));

        service.syncProducts();

        // 490 comes first and fails; 388 after it is still saved.
        assertThat(colours(gpu)).containsExactly("Бял");
        assertThat(productRepository.findByExternalIdIn(List.of(50001L, 50002L))).isEmpty();
        // The refused chunk keeps what it had, and as it was seen in the API it is not marked unavailable.
        assertThat(colours(drive)).containsExactly("Черен");
        assertThat(productRepository.findById(drive.getId()).orElseThrow().getStatus())
                .isEqualTo(ProductStatus.AVAILABLE);
        SyncLog log = lastLog();
        assertThat(log.getStatus()).isEqualTo("FAILED");
        assertThat(log.getErrorMessage()).contains("FailedChunks: 1");
    }

    @Test
    @DisplayName("Един и същ запис два пъти в партидата се синхронизира веднъж")
    void itemListedTwiceIsSyncedOnce() {
        Product drive = existingProduct(33385L, "WD-HDD-PASSPORT-6TB-WH", externalDrives);
        when(valiApiService.getProductsByCategory(490L)).thenReturn(List.of(
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", WHITE),
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", BLACK)));

        service.syncProducts();

        assertThat(colours(drive)).containsExactly("Бял");
        SyncLog log = lastLog();
        assertThat(log.getStatus()).isEqualTo("SUCCESS");
        assertThat(log.getErrorMessage()).contains("Skipped: 1");
    }

    @Test
    @DisplayName("Ръчен статус остава, когато Вали дава друг: „Наличен“ остава видим, „Неналичен“ — скрит")
    void manualStatusWinsOverFeed() {
        Product manualAvailable = existingProduct(33385L, "WD-HDD-PASSPORT-6TB-WH", externalDrives);
        manualAvailable.setManualStatus(true);
        productRepository.save(manualAvailable);

        Product manualNotAvailable = existingProduct(33386L, "WD-HDD-PASSPORT-5TB-WH", externalDrives);
        manualNotAvailable.setStatus(ProductStatus.NOT_AVAILABLE);
        manualNotAvailable.setShow(false);
        manualNotAvailable.setManualStatus(true);
        productRepository.save(manualNotAvailable);

        Product fromFeed = existingProduct(33387L, "WD-HDD-PASSPORT-4TB-WH", externalDrives);

        when(valiApiService.getProductsByCategory(490L)).thenReturn(List.of(
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", WHITE, ProductStatus.NOT_AVAILABLE),
                item(33386L, "WD-HDD-PASSPORT-5TB-WH", WHITE, ProductStatus.AVAILABLE),
                item(33387L, "WD-HDD-PASSPORT-4TB-WH", WHITE, ProductStatus.NOT_AVAILABLE)));

        service.syncProducts();

        Product a = productRepository.findById(manualAvailable.getId()).orElseThrow();
        assertThat(a.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(a.getShow()).isTrue();

        Product n = productRepository.findById(manualNotAvailable.getId()).orElseThrow();
        assertThat(n.getStatus()).isEqualTo(ProductStatus.NOT_AVAILABLE);
        assertThat(n.getShow()).isFalse();

        Product f = productRepository.findById(fromFeed.getId()).orElseThrow();
        assertThat(f.getStatus()).isEqualTo(ProductStatus.NOT_AVAILABLE);
        assertThat(f.getShow()).isFalse();
    }

    @Test
    @DisplayName("Продукт с ръчен „Наличен“ остава наличен, когато Вали го махне от каталога си")
    void manualStatusSurvivesLeavingTheFeed() {
        existingProduct(33385L, "WD-HDD-PASSPORT-6TB-WH", externalDrives);

        Product manual = existingProduct(33386L, "WD-HDD-PASSPORT-5TB-WH", externalDrives);
        manual.setManualStatus(true);
        manual.setShow(true);
        productRepository.save(manual);

        Product gone = existingProduct(33387L, "WD-HDD-PASSPORT-4TB-WH", externalDrives);

        when(valiApiService.getProductsByCategory(490L)).thenReturn(List.of(
                item(33385L, "WD-HDD-PASSPORT-6TB-WH", WHITE)));

        service.syncProducts();

        Product m = productRepository.findById(manual.getId()).orElseThrow();
        assertThat(m.getStatus()).isEqualTo(ProductStatus.AVAILABLE);
        assertThat(m.getShow()).isTrue();
        assertThat(productRepository.findById(gone.getId()).orElseThrow().getStatus())
                .isEqualTo(ProductStatus.NOT_AVAILABLE);
    }

    private Category category(String name, long externalId) {
        Category c = new Category();
        c.setNameBg(name);
        c.setSlug("cat-" + externalId);
        c.setExternalId(externalId);
        c.setPlatform(Platform.VALI);
        c.setShow(true);
        return categoryRepository.save(c);
    }

    private ParameterOption option(String name, long externalId) {
        ParameterOption o = new ParameterOption();
        o.setNameBg(name);
        o.setExternalId(externalId);
        o.setParameter(colour);
        return parameterOptionRepository.save(o);
    }

    /** Available and black, the way it sat in production before the run. */
    private Product existingProduct(long externalId, String referenceNumber, Category category) {
        Product p = new Product();
        p.setNameBg(referenceNumber);
        p.setExternalId(externalId);
        p.setReferenceNumber(referenceNumber);
        p.setPlatform(Platform.VALI);
        p.setStatus(ProductStatus.AVAILABLE);
        p.setCategory(category);
        p.setManufacturer(manufacturer);
        p.setProductParameters(new HashSet<>());

        ProductParameter pp = new ProductParameter();
        pp.setProduct(p);
        pp.setParameter(colour);
        pp.setParameterOption(black);
        p.getProductParameters().add(pp);
        return productRepository.save(p);
    }

    private static ProductRequestDto item(long id, String referenceNumber, long colourOption) {
        return item(id, referenceNumber, colourOption, ProductStatus.AVAILABLE);
    }

    private static ProductRequestDto item(long id, String referenceNumber, long colourOption, ProductStatus status) {
        NameDto name = new NameDto();
        name.setLanguageCode("bg");
        name.setText(referenceNumber);

        ParameterValueRequestDto colour = new ParameterValueRequestDto();
        colour.setParameterId(COLOUR);
        colour.setOptionId(colourOption);

        ProductRequestDto dto = new ProductRequestDto();
        dto.setId(id);
        dto.setReferenceNumber(referenceNumber);
        dto.setManufacturerId(WD);
        dto.setStatus(status.getCode());
        dto.setShow(true);
        dto.setPriceClient(new BigDecimal("181.71"));
        dto.setName(List.of(name));
        dto.setParameters(List.of(colour));
        return dto;
    }

    private List<String> colours(Product product) {
        return jdbc.queryForList("SELECT o.name_bg FROM product_parameters pp "
                + "JOIN parameter_options o ON o.id = pp.parameter_option_id "
                + "WHERE pp.product_id = ? ORDER BY o.name_bg", String.class, product.getId());
    }

    private SyncLog lastLog() {
        return syncLogRepository.findAll().stream()
                .filter(l -> "VALI_PRODUCTS".equals(l.getSyncType()))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("no VALI_PRODUCTS row in sync_logs"));
    }
}
