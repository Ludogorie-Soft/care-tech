package com.techstore.service;

import com.techstore.service.sync.AsbisSyncService;
import com.techstore.service.sync.MostSyncService;
import com.techstore.service.sync.TekraSyncService;
import com.techstore.service.sync.ValiSyncService;
import com.techstore.util.Markers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronJobService {

    private final ValiSyncService valiSyncService;
    private final TekraSyncService tekraSyncService;
    private final MostSyncService mostSyncService;
    private final AsbisSyncService asbisSyncService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void syncApis() {
        log.info("Starting scheduled synchronization at {}", LocalDateTime.now());

        // --- Vali ---
        try {
            valiSyncService.syncParameters();
            log.info("Vali parameters sync completed at {}", LocalDateTime.now());

            valiSyncService.syncProducts();
            log.info("Vali products sync completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error(Markers.CRITICAL, "Scheduled Vali synchronization failed", e);
        }

        // --- Tekra ---
        try {
            tekraSyncService.syncTekraParameters();
            log.info("Tekra parameters sync completed at {}", LocalDateTime.now());

            tekraSyncService.syncTekraProducts();
            log.info("Tekra products sync completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error(Markers.CRITICAL, "Scheduled Tekra synchronization failed", e);
        }

        // --- Most ---
        try {
            mostSyncService.syncMostManufacturers();
            log.info("Most manufacturers sync completed at {}", LocalDateTime.now());

            mostSyncService.syncMostParameters();
            log.info("Most parameters sync completed at {}", LocalDateTime.now());

            mostSyncService.syncMostProducts();
            log.info("Most products sync completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error(Markers.CRITICAL, "Scheduled Most synchronization failed", e);
        }

        // --- Asbis ---
        try {
            asbisSyncService.syncAsbisParameters();
            log.info("Asbis parameters sync completed at {}", LocalDateTime.now());

            asbisSyncService.syncAsbisProducts();
            log.info("Asbis products sync completed at {}", LocalDateTime.now());

            asbisSyncService.syncAsbisPriceAvail();
            log.info("Asbis price/availability sync completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error(Markers.CRITICAL, "Scheduled Asbis synchronization failed", e);
        }

        log.info("Scheduled synchronization finished at {}", LocalDateTime.now());
    }
}
