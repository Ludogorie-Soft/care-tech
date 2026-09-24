package com.techstore.service.filter;

import java.util.Map;

/**
 * Outcome of one {@link FilterIndexService#rebuild} call.
 *
 * @param status SUCCESS, DRY_RUN (everything rolled back) or SKIPPED (another rebuild held the lock)
 * @param stats  counts that make a run comparable with the previous one; see the service for keys
 */
public record FilterRebuildResult(String status, String trigger, boolean dryRun, long durationMs,
                                  Map<String, Object> stats) {

    static FilterRebuildResult skipped(String trigger, boolean dryRun) {
        return new FilterRebuildResult("SKIPPED", trigger, dryRun, 0, Map.of());
    }
}
