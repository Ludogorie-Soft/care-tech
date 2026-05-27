package com.techstore.dto.response;

import com.techstore.entity.SyncLog;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SyncLogResponseDTO {

    private final Long id;
    private final String syncType;
    private final String status;
    private final Long recordsProcessed;
    private final Long recordsCreated;
    private final Long recordsUpdated;
    private final String errorMessage;
    private final Long durationMs;
    private final LocalDateTime createdAt;

    public SyncLogResponseDTO(SyncLog syncLog) {
        this.id = syncLog.getId();
        this.syncType = syncLog.getSyncType();
        this.status = syncLog.getStatus();
        this.recordsProcessed = syncLog.getRecordsProcessed();
        this.recordsCreated = syncLog.getRecordsCreated();
        this.recordsUpdated = syncLog.getRecordsUpdated();
        this.errorMessage = syncLog.getErrorMessage();
        this.durationMs = syncLog.getDurationMs();
        this.createdAt = syncLog.getCreatedAt();
    }
}
