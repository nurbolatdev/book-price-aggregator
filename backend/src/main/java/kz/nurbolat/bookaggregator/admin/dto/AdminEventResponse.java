package kz.nurbolat.bookaggregator.admin.dto;

import kz.nurbolat.bookaggregator.admin.SystemEvent;

import java.time.Instant;
import java.util.Map;

public record AdminEventResponse(Long id, SystemEvent.Level level, String source, String message,
                                 Map<String, Object> details, Instant createdAt) {

    public static AdminEventResponse from(SystemEvent event) {
        return new AdminEventResponse(event.getId(), event.getLevel(), event.getSource(), event.getMessage(),
                event.getDetails(), event.getCreatedAt());
    }
}
