package kz.nurbolat.bookaggregator.beacon;

public enum BeaconCondition {
    /** Any drop below the baseline price. */
    ANY_DROP,
    /** Best price at or below {@code targetPrice}. */
    BELOW_PRICE,
    /** Discount of at least {@code targetPercent} from the baseline price. */
    DISCOUNT_PERCENT
}
