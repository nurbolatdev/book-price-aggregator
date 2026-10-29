package kz.nurbolat.bookaggregator.beacon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(description = "Id издания", example = "42") @NotNull(message = "Обязательное поле") Long editionId,
        @Schema(description = "Тип условия: ANY_DROP, BELOW_PRICE или DISCOUNT_PERCENT") @NotNull(message = "Обязательное поле") BeaconCondition conditionType,
        @Schema(description = "Целевая цена в тенге для BELOW_PRICE", example = "4500")
        @DecimalMin(value = "0.01", message = "должно быть больше 0") @DecimalMax(value = "10000000", message = "Не больше 10000000")
        @Digits(integer = 8, fraction = 2, message = "Не больше 8 цифр до запятой и 2 после") BigDecimal targetPrice,
        @Schema(description = "Падение цены в процентах для DISCOUNT_PERCENT", example = "20")
        @Min(value = 1, message = "Не меньше 1") @Max(value = 90, message = "Не больше 90") Integer targetPercent
) implements BeaconConditionFields {
}
