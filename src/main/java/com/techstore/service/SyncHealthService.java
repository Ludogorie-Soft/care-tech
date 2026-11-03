package com.techstore.service;

import com.techstore.dto.response.SyncHealthDto;
import com.techstore.entity.SyncLog;
import com.techstore.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.techstore.util.LogHelper.LOG_STATUS_SUCCESS;

/**
 * Tells whether every nightly sync has succeeded recently. The sync log list alone cannot: from
 * 23.10 to 03.11.2026 VALI_PRODUCTS rolled back together with its own log row, so the list showed
 * only successful runs while the VALI prices and stock were twelve days old.
 */
@Service
@RequiredArgsConstructor
public class SyncHealthService {

    /** What CronJobService.syncApis writes every night, in that order. */
    static final List<String> NIGHTLY_SYNC_TYPES = List.of(
            "VALI_MANUFACTURERS", "VALI_PARAMETERS", "VALI_PRODUCTS",
            "TEKRA_PARAMETERS", "TEKRA_PRODUCTS",
            "MOST_MANUFACTURERS", "MOST_PARAMETERS", "MOST_PRODUCTS",
            "ASBIS_PARAMETERS", "ASBIS_PRODUCTS", "ASBIS_PRICE_AVAIL");

    /** The run starts at 01:00 and takes about 25 minutes: a day plus two hours of slack. */
    static final Duration MAX_AGE = Duration.ofHours(26);

    private final SyncLogRepository syncLogRepository;

    @Transactional(readOnly = true)
    public List<SyncHealthDto> nightlyHealth() {
        return nightlyHealth(LocalDateTime.now());
    }

    List<SyncHealthDto> nightlyHealth(LocalDateTime now) {
        LocalDateTime oldestAccepted = now.minus(MAX_AGE);
        return NIGHTLY_SYNC_TYPES.stream()
                .map(type -> {
                    Optional<SyncLog> last = syncLogRepository.findTopBySyncTypeOrderByCreatedAtDesc(type);
                    LocalDateTime lastSuccessAt = syncLogRepository
                            .findTopBySyncTypeAndStatusOrderByCreatedAtDesc(type, LOG_STATUS_SUCCESS)
                            .map(SyncLog::getCreatedAt)
                            .orElse(null);
                    return new SyncHealthDto(
                            type,
                            last.map(SyncLog::getCreatedAt).orElse(null),
                            last.map(SyncLog::getStatus).orElse(null),
                            lastSuccessAt,
                            lastSuccessAt == null || lastSuccessAt.isBefore(oldestAccepted));
                })
                .toList();
    }
}
