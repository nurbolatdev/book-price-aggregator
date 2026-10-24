package kz.nurbolat.bookaggregator.admin.dto;

import java.time.LocalDate;

public record AdminActivityDay(LocalDate date, long searches, long registrations, long beacons, long emails) {
}
