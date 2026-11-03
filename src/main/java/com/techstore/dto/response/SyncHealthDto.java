package com.techstore.dto.response;

import java.time.LocalDateTime;

/**
 * One nightly sync type: its latest run, its latest successful run, and whether that success is
 * too old. The latest run is null when the type has never written a row.
 */
public record SyncHealthDto(String syncType,
                            LocalDateTime lastRunAt,
                            String lastStatus,
                            LocalDateTime lastSuccessAt,
                            boolean overdue) {
}
