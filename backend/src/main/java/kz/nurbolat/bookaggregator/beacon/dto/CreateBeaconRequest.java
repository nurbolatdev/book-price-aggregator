package kz.nurbolat.bookaggregator.beacon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kz.nurbolat.bookaggregator.beacon.BeaconCondition;

import java.math.BigDecimal;

/**
 * @param targetPrice   for {@code BELOW_PRICE}: notify when the best price is at or below it
 * @param targetPercent for {@code DISCOUNT_PERCENT}: notify when the price fell this many percent from the baseline
 */
@ValidBeaconCondition
public record CreateBeaconRequest(
        @NotNull Long editionId,
        @NotNull BeaconCondition conditionType,
        @DecimalMin(value = "0.01", message = "должно быть больше 0") @DecimalMax("10000000")
        @Digits(integer = 8, fraction = 2) BigDecimal targetPrice,
        @Min(1) @Max(90) Integer targetPercent
) implements BeaconConditionFields {
}
