package kz.nurbolat.bookaggregator.admin.dto;

import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.time.Instant;

/**
 * @param successPercent24h pages loaded without error over the last 24 hours, null if no pages were loaded
 * @param avgRunSeconds     average duration of finished runs over the last 7 days, null if there were none
 * @param circuitBreaker    CLOSED, OPEN, HALF_OPEN, ...; CLOSED for a store that has not been requested yet
 */
public record AdminParserResponse(
        StoreCode code,
        String name,
        boolean enabled,
        boolean hasParser,
        LastRun lastRun,
        Double successPercent24h,
        long matchedOffers,
        Double avgRunSeconds,
        String circuitBreaker
) {

    /** @param durationSeconds null while the run is in progress */
    public record LastRun(RunStatus status, Instant startedAt, Long durationSeconds) {
    }

    public record StoreState(StoreCode code, boolean enabled) {
    }
}
