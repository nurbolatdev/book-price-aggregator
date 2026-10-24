package kz.nurbolat.bookaggregator.admin.dto;

import java.util.Map;

/**
 * @param status     worst status of the components: UP, DOWN, OUT_OF_SERVICE or UNKNOWN
 * @param components status by name: db, redis, rabbit, mail, scheduler
 */
public record AdminHealthResponse(String status, Map<String, ComponentHealth> components, Metrics metrics) {

    /** @param details short text of a failure, if any */
    public record ComponentHealth(String status, String details) {
    }

    /** A value is null when its metric is not available (nothing measured yet, Redis is down). */
    public record Metrics(Double httpP95Millis, Integer dbPoolActive, Integer dbPoolMax, Long redisUsedMemoryBytes) {
    }
}
