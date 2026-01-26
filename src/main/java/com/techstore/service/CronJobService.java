package com.techstore.service;

import com.techstore.service.sync.TekraSyncService;
import com.techstore.service.sync.ValiSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronJobService {

    private final ValiSyncService valiSyncService;
    private final TekraSyncService tekraSyncService;

//    @Scheduled(cron = "0 0 1 * * ?")
    public void syncApis() {
        log.info("Starting scheduled Vali synchronization at {}", LocalDateTime.now());
        try {
            valiSyncService.syncParameters();
            log.info("Scheduled parameters synchronization completed at {}", LocalDateTime.now());

            valiSyncService.syncProducts();
            log.info("Scheduled products synchronization completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error("CRITICAL: Scheduled Vali synchronization failed", e);
        }

        try {
            tekraSyncService.syncTekraParameters();
            log.info("Scheduled Tekra parameters synchronization completed at {}", LocalDateTime.now());

            tekraSyncService.syncTekraProducts();
            log.info("Scheduled Tekra products synchronization completed at {}", LocalDateTime.now());

        } catch (Exception e) {
            log.error("CRITICAL: Scheduled Tekra synchronization failed", e);
        }
    }
}
