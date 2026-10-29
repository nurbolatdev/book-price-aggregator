package kz.nurbolat.bookaggregator.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record StoreToggleRequest(@Schema(description = "Включить (true) или выключить (false) магазин", example = "false") @NotNull(message = "Обязательное поле") Boolean enabled) {
}
