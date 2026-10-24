package kz.nurbolat.bookaggregator.admin.dto;

import java.time.Instant;

public record AdminUserResponse(Long id, String email, String name, Instant createdAt, boolean verified,
                                long beacons) {
}
