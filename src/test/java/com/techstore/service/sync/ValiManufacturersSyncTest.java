package com.techstore.service.sync;

import com.techstore.dto.request.ManufacturerRequestDto;
import com.techstore.entity.Manufacturer;
import com.techstore.enums.Platform;
import com.techstore.repository.CategoryRepository;
import com.techstore.repository.ManufacturerRepository;
import com.techstore.repository.ParameterOptionRepository;
import com.techstore.repository.ParameterRepository;
import com.techstore.repository.ProductRepository;
import com.techstore.service.ValiApiService;
import com.techstore.util.LogHelper;
import com.techstore.util.SyncHelper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Every night VALI_PRODUCTS reported "Errors: 33": 33 VALI products whose manufacturer had no
 * VALI id in our database, so the products sync skipped them and they never reached the shop.
 * Three brands were new (the manufacturers sync was not in the nightly cron), and three already
 * existed by name — SCUF and Inno3D from ASBIS, Ubiquiti under an id VALI had dropped — which the
 * manufacturers sync skipped without recording VALI's id.
 */
@ExtendWith(MockitoExtension.class)
class ValiManufacturersSyncTest {

    @Mock private ValiApiService valiApiService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ManufacturerRepository manufacturerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private ParameterOptionRepository parameterOptionRepository;
    @Mock private EntityManager entityManager;
    @Mock private SyncHelper syncHelper;
    @Mock private LogHelper logHelper;

    private ValiSyncService service;

    private Manufacturer scufFromAsbis;
    private Manufacturer ubiquitiOldId;
    private Manufacturer kingston;
    private Manufacturer dual;

    @BeforeEach
    void setUp() {
        service = new ValiSyncService(valiApiService, categoryRepository, manufacturerRepository,
                productRepository, parameterRepository, parameterOptionRepository, entityManager,
                syncHelper, logHelper);

        scufFromAsbis = manufacturer(459L, null, "SCUF", Platform.ASBIS);
        ubiquitiOldId = manufacturer(328L, 191L, "UBIQUITI", Platform.VALI);
        kingston = manufacturer(10L, 66L, "Kingston", Platform.VALI);
        dual = manufacturer(11L, 301L, "Dual", Platform.VALI);
        when(manufacturerRepository.findAll()).thenReturn(List.of(scufFromAsbis, ubiquitiOldId, kingston, dual));
        lenient().when(manufacturerRepository.save(any(Manufacturer.class))).thenAnswer(inv -> inv.getArgument(0));

        when(valiApiService.getManufacturers()).thenReturn(List.of(
                dto(518L, "SCUF"),
                dto(289L, "Ubiquiti"),
                dto(522L, "Natec"),
                dto(66L, "Kingston"),
                dto(301L, "Dual"),
                dto(300L, "Dual")));

        service.syncManufacturers();
    }

    @Test
    @DisplayName("A brand another supplier already brought in gets VALI's id")
    void linksBrandFromAnotherSupplier() {
        assertEquals(518L, scufFromAsbis.getExternalId());
        assertEquals(Platform.ASBIS, scufFromAsbis.getPlatform());
    }

    @Test
    @DisplayName("A brand under an id VALI no longer lists moves to the new id")
    void relinksDroppedId() {
        assertEquals(289L, ubiquitiOldId.getExternalId());
    }

    @Test
    @DisplayName("A new brand is created as a VALI manufacturer")
    void createsNewBrand() {
        ArgumentCaptor<Manufacturer> saved = ArgumentCaptor.forClass(Manufacturer.class);
        verify(manufacturerRepository, times(3)).save(saved.capture());

        Manufacturer natec = saved.getAllValues().stream()
                .filter(m -> "Natec".equals(m.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(522L, natec.getExternalId());
        assertEquals(Platform.VALI, natec.getPlatform());
    }

    @Test
    @DisplayName("A name match whose id VALI still lists is left alone")
    void keepsIdStillInUse() {
        assertEquals(301L, dual.getExternalId());
        assertEquals(66L, kingston.getExternalId());
    }

    private static Manufacturer manufacturer(Long id, Long externalId, String name, Platform platform) {
        Manufacturer m = new Manufacturer();
        m.setId(id);
        m.setExternalId(externalId);
        m.setName(name);
        m.setPlatform(platform);
        return m;
    }

    private static ManufacturerRequestDto dto(Long id, String name) {
        ManufacturerRequestDto d = new ManufacturerRequestDto();
        d.setId(id);
        d.setName(name);
        return d;
    }
}
