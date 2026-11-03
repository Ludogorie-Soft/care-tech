package com.techstore.service;

import com.techstore.dto.response.SyncHealthDto;
import com.techstore.entity.SyncLog;
import com.techstore.repository.SyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncHealthService — автоматична синхронизация без успех от над 8 ч")
class SyncHealthServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 10, 0);
    private static final LocalDateTime THIS_MORNING = LocalDateTime.of(2026, 11, 3, 5, 5);

    @Mock SyncLogRepository syncLogRepository;

    private SyncHealthService service;

    @BeforeEach
    void setUp() {
        service = new SyncHealthService(syncLogRepository);
        // Unless a test says otherwise, every type succeeded in the 05:00 run.
        lenient().when(syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc(anyString()))
                .thenAnswer(inv -> Optional.of(log(inv.getArgument(0), "SUCCESS", THIS_MORNING)));
        lenient().when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc(anyString(), eq("SUCCESS")))
                .thenAnswer(inv -> Optional.of(log(inv.getArgument(0), "SUCCESS", THIS_MORNING)));
    }

    @Test
    @DisplayName("Всички типове, в реда на cron-а; при успех в 05:00 нищо не закъснява")
    void everyNightlyTypeInCronOrder() {
        List<SyncHealthDto> health = service.health(NOW);

        assertThat(health).extracting(SyncHealthDto::syncType).containsExactlyElementsOf(SyncHealthService.SCHEDULED_SYNC_TYPES);
        assertThat(health).noneMatch(SyncHealthDto::overdue);
    }

    @Test
    @DisplayName("VALI_PRODUCTS: последен успех на 22.10 → закъснява, дори без нито един FAILED ред")
    void lastSuccessTwelveDaysAgoIsOverdue() {
        LocalDateTime oct22 = LocalDateTime.of(2026, 10, 22, 1, 6);
        when(syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc("VALI_PRODUCTS"))
                .thenReturn(Optional.of(log("VALI_PRODUCTS", "SUCCESS", oct22)));
        when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc("VALI_PRODUCTS", "SUCCESS"))
                .thenReturn(Optional.of(log("VALI_PRODUCTS", "SUCCESS", oct22)));

        SyncHealthDto vali = byType(service.health(NOW), "VALI_PRODUCTS");

        assertThat(vali.overdue()).isTrue();
        assertThat(vali.lastSuccessAt()).isEqualTo(oct22);
        assertThat(byType(service.health(NOW), "TEKRA_PRODUCTS").overdue()).isFalse();
    }

    @Test
    @DisplayName("Прогонът в 05:00 е FAILED, а успехът е от 23:00 → закъснява и показва FAILED")
    void failedThisMorningAfterSuccessAtEleven() {
        when(syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc("MOST_PRODUCTS"))
                .thenReturn(Optional.of(log("MOST_PRODUCTS", "FAILED", THIS_MORNING)));
        when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc("MOST_PRODUCTS", "SUCCESS"))
                .thenReturn(Optional.of(log("MOST_PRODUCTS", "SUCCESS", THIS_MORNING.minusHours(6))));

        SyncHealthDto most = byType(service.health(NOW), "MOST_PRODUCTS");

        assertThat(most.overdue()).isTrue();
        assertThat(most.lastStatus()).isEqualTo("FAILED");
        assertThat(most.lastRunAt()).isEqualTo(THIS_MORNING);
    }

    @Test
    @DisplayName("Тип без нито един ред → закъснява, без дати")
    void neverRun() {
        when(syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc("ASBIS_PRICE_AVAIL")).thenReturn(Optional.empty());
        when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc("ASBIS_PRICE_AVAIL", "SUCCESS"))
                .thenReturn(Optional.empty());

        SyncHealthDto asbis = byType(service.health(NOW), "ASBIS_PRICE_AVAIL");

        assertThat(asbis.overdue()).isTrue();
        assertThat(asbis.lastRunAt()).isNull();
        assertThat(asbis.lastStatus()).isNull();
        assertThat(asbis.lastSuccessAt()).isNull();
    }

    @Test
    @DisplayName("Границата е 8 ч: 7:59 е наред, 8:01 закъснява")
    void eightHourBoundary() {
        when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc("VALI_PARAMETERS", "SUCCESS"))
                .thenReturn(Optional.of(log("VALI_PARAMETERS", "SUCCESS", NOW.minusHours(7).minusMinutes(59))));
        when(syncLogRepository.findTopBySyncTypeAndStatusOrderByCreatedAtDesc("VALI_MANUFACTURERS", "SUCCESS"))
                .thenReturn(Optional.of(log("VALI_MANUFACTURERS", "SUCCESS", NOW.minusHours(8).minusMinutes(1))));

        List<SyncHealthDto> health = service.health(NOW);

        assertThat(byType(health, "VALI_PARAMETERS").overdue()).isFalse();
        assertThat(byType(health, "VALI_MANUFACTURERS").overdue()).isTrue();
    }

    private static SyncLog log(String type, String status, LocalDateTime at) {
        SyncLog log = new SyncLog();
        log.setSyncType(type);
        log.setStatus(status);
        log.setCreatedAt(at);
        return log;
    }

    private static SyncHealthDto byType(List<SyncHealthDto> health, String type) {
        return health.stream().filter(h -> h.syncType().equals(type)).findFirst().orElseThrow();
    }
}
