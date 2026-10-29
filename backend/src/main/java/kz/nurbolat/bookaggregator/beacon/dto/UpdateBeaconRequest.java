package kz.nurbolat.bookaggregator.beacon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.beacon.BeaconCondition;
import kz.nurbolat.bookaggregator.beacon.BeaconStatus;

import java.math.BigDecimal;

/**
 * A new condition (all its fields together) and/or a new status; what is left out stays as it is.
 *
 * @param status {@code ACTIVE} or {@code PAUSED}; {@code TRIGGERED} is set only by the price check
 */
@ValidBeaconCondition(typeOptional = true)
public record UpdateBeaconRequest(
        @Schema(description = "Новый тип условия; передаётся вместе с его значением") BeaconCondition conditionType,
        @Schema(description = "Целевая цена в тенге для BELOW_PRICE", example = "4500")
        @DecimalMin(value = "0.01", message = "должно быть больше 0") @DecimalMax(value = "10000000", message = "Не больше 10000000")
        @Digits(integer = 8, fraction = 2, message = "Не больше 8 цифр до запятой и 2 после") BigDecimal targetPrice,
        @Schema(description = "Падение цены в процентах для DISCOUNT_PERCENT", example = "20")
        @Min(value = 1, message = "Не меньше 1") @Max(value = 90, message = "Не больше 90") Integer targetPercent,
        @Schema(description = "Новый статус: ACTIVE или PAUSED") BeaconStatus status
) implements BeaconConditionFields {

    @AssertTrue(message = "можно поставить только ACTIVE или PAUSED")
    boolean isStatus() {
        return status != BeaconStatus.TRIGGERED;
    }
}
