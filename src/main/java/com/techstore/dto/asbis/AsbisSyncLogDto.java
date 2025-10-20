package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisSyncLogDto {
    private Long id;
    private String syncType;
    private String status;
    private Long recordsProcessed;
    private Long recordsCreated;
    private Long recordsUpdated;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime createdAt;
}
