package kz.nurbolat.bookaggregator.beacon;

import java.math.BigDecimal;

/**
 * Whether a beacon fires at a best price, and whether that deserves an email.
 */
public final class BeaconConditionEvaluator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private BeaconConditionEvaluator() {
    }

    /**
     * {@code ANY_DROP}: below the baseline; {@code BELOW_PRICE}: at or below the target;
     * {@code DISCOUNT_PERCENT}: at least the given percent below the baseline. A condition that needs a baseline
     * is never met without one.
     */
    public static boolean isSatisfied(Beacon beacon, BigDecimal best) {
        BigDecimal baseline = beacon.getBaselinePrice();
        return switch (beacon.getConditionType()) {
            case ANY_DROP -> baseline != null && best.compareTo(baseline) < 0;
            case BELOW_PRICE -> best.compareTo(beacon.getTargetPrice()) <= 0;
            // best <= baseline * (100 - p) / 100, compared without rounding
            case DISCOUNT_PERCENT -> baseline != null && best.multiply(HUNDRED)
                    .compareTo(baseline.multiply(HUNDRED.subtract(BigDecimal.valueOf(beacon.getTargetPercent())))) <= 0;
        };
    }

    /**
     * Repeat guard: the same price, or a higher one after a drop, is not reported twice.
     */
    public static boolean isNewLow(Beacon beacon, BigDecimal best) {
        return beacon.getLastNotifiedPrice() == null || best.compareTo(beacon.getLastNotifiedPrice()) < 0;
    }
}
