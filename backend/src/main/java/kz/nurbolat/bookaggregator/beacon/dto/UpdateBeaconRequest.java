package kz.nurbolat.bookaggregator.beacon.dto;

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
        BeaconCondition conditionType,
        @DecimalMin(value = "0.01", message = "должно быть больше 0") @DecimalMax("10000000")
        @Digits(integer = 8, fraction = 2) BigDecimal targetPrice,
        @Min(1) @Max(90) Integer targetPercent,
        BeaconStatus status
) implements BeaconConditionFields {

    @AssertTrue(message = "можно поставить только ACTIVE или PAUSED")
    boolean isStatus() {
        return status != BeaconStatus.TRIGGERED;
    }
}
