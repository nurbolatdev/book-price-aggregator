package kz.nurbolat.bookaggregator.edition.dto;

import java.math.BigDecimal;

/**
 * How the current best price compares with the 90-day average of daily minimums.
 *
 * @param diffPercent rounded difference from the average, negative when cheaper
 */
public record PriceVerdict(BigDecimal avg90d, Integer diffPercent, Label label) {

    public enum Label {
        BELOW_AVERAGE,
        AVERAGE,
        ABOVE_AVERAGE,
        NOT_ENOUGH_DATA
    }

    public static PriceVerdict notEnoughData(BigDecimal avg90d) {
        return new PriceVerdict(avg90d, null, Label.NOT_ENOUGH_DATA);
    }
}
