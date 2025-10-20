package com.techstore.dto.asbis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsbisSyncResultDto {
    private Boolean success;
    private String message;
    private Long totalProcessed;
    private Long created;
    private Long updated;
    private Long skipped;
    private Long errors;
    private Long durationMs;
    private List<String> errorDetails;
}
