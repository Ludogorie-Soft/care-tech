package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ============================================
// ASBIS SYNC STATUS DTO
// ============================================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisSyncStatusDto {
    private Boolean enabled;
    private Boolean connected;
    private LocalDateTime lastSyncTime;
    private AsbisSyncStats stats;
    private List<AsbisSyncLogDto> recentLogs;
}