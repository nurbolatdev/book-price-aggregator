package kz.nurbolat.bookaggregator.admin.dto;

import jakarta.validation.constraints.NotNull;

public record StoreToggleRequest(@NotNull Boolean enabled) {
}
