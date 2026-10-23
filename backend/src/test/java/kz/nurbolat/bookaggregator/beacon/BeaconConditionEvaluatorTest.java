package kz.nurbolat.bookaggregator.beacon;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static kz.nurbolat.bookaggregator.beacon.BeaconConditionEvaluator.isNewLow;
import static kz.nurbolat.bookaggregator.beacon.BeaconConditionEvaluator.isSatisfied;
import static org.assertj.core.api.Assertions.assertThat;

class BeaconConditionEvaluatorTest {

    @Test
    void shouldFireAnyDropOnlyWhenPriceIsBelowBaseline() {
        Beacon beacon = beacon(BeaconCondition.ANY_DROP, "3610.00", null, null);

        assertThat(isSatisfied(beacon, price("3609.99"))).isTrue();
        assertThat(isSatisfied(beacon, price("3610.00"))).isFalse();
        assertThat(isSatisfied(beacon, price("3700.00"))).isFalse();
    }

    @Test
    void shouldFireBelowPriceWhenPriceEqualsOrIsUnderTarget() {
        Beacon beacon = beacon(BeaconCondition.BELOW_PRICE, "3610.00", "3200.00", null);

        assertThat(isSatisfied(beacon, price("3200.00"))).isTrue();
        assertThat(isSatisfied(beacon, price("3150.00"))).isTrue();
        assertThat(isSatisfied(beacon, price("3200.01"))).isFalse();
    }

    @Test
    void shouldFireBelowPriceWithoutBaseline() {
        Beacon beacon = beacon(BeaconCondition.BELOW_PRICE, null, "3200.00", null);

        assertThat(isSatisfied(beacon, price("3100.00"))).isTrue();
    }

    @Test
    void shouldFireDiscountWhenExactlyTheGivenPercentIsReached() {
        Beacon beacon = beacon(BeaconCondition.DISCOUNT_PERCENT, "3000.00", null, 10);

        assertThat(isSatisfied(beacon, price("2700.00"))).isTrue();
        assertThat(isSatisfied(beacon, price("2699.99"))).isTrue();
        assertThat(isSatisfied(beacon, price("2700.01"))).isFalse();
    }

    @Test
    void shouldCompareDiscountWithoutRoundingWhenThresholdHasManyDecimals() {
        // 15% off 3333.33 is 2833.3305: 2833.33 qualifies, 2833.34 does not
        Beacon beacon = beacon(BeaconCondition.DISCOUNT_PERCENT, "3333.33", null, 15);

        assertThat(isSatisfied(beacon, price("2833.33"))).isTrue();
        assertThat(isSatisfied(beacon, price("2833.34"))).isFalse();
    }

    @Test
    void shouldNotFireBaselineConditionsWithoutBaseline() {
        assertThat(isSatisfied(beacon(BeaconCondition.ANY_DROP, null, null, null), price("1.00"))).isFalse();
        assertThat(isSatisfied(beacon(BeaconCondition.DISCOUNT_PERCENT, null, null, 10), price("1.00"))).isFalse();
    }

    @Test
    void shouldReportOnlyPricesLowerThanTheLastReportedOne() {
        Beacon beacon = beacon(BeaconCondition.ANY_DROP, "3610.00", null, null);
        assertThat(isNewLow(beacon, price("3500.00"))).isTrue();

        beacon.setLastNotifiedPrice(price("3500.00"));

        assertThat(isNewLow(beacon, price("3500.00"))).isFalse();
        assertThat(isNewLow(beacon, price("3550.00"))).isFalse();
        assertThat(isNewLow(beacon, price("3499.99"))).isTrue();
    }

    private static Beacon beacon(BeaconCondition type, String baseline, String target, Integer percent) {
        Beacon beacon = new Beacon();
        beacon.setConditionType(type);
        beacon.setBaselinePrice(baseline == null ? null : price(baseline));
        beacon.setTargetPrice(target == null ? null : price(target));
        beacon.setTargetPercent(percent == null ? null : percent.shortValue());
        return beacon;
    }

    private static BigDecimal price(String value) {
        return new BigDecimal(value);
    }
}
