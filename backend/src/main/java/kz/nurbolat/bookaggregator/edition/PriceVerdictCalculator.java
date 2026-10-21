package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict;
import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict.Label;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Compares the current best price with the average of daily minimums over the last 90 days.
 * Within ±5% the price is average; with less than 14 days of history there is no verdict.
 */
@Component
public class PriceVerdictCalculator {

    static final int PERIOD_DAYS = 90;
    static final int MIN_HISTORY_DAYS = 14;
    private static final int THRESHOLD_PERCENT = 5;

    /**
     * @param dailyMinimums daily minimums of the last {@value #PERIOD_DAYS} days, oldest first
     * @param currentPrice  current best in-stock price, {@code null} if nothing is in stock
     */
    public PriceVerdict calculate(List<DailyPrice> dailyMinimums, BigDecimal currentPrice, LocalDate today) {
        if (dailyMinimums.isEmpty()) {
            return PriceVerdict.notEnoughData(null);
        }
        BigDecimal average = dailyMinimums.stream()
                .map(DailyPrice::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(dailyMinimums.size()), 2, RoundingMode.HALF_UP);

        long historyDays = ChronoUnit.DAYS.between(dailyMinimums.get(0).date(), today);
        if (historyDays < MIN_HISTORY_DAYS || currentPrice == null || average.signum() == 0) {
            return PriceVerdict.notEnoughData(average);
        }

        int diffPercent = currentPrice.subtract(average)
                .multiply(BigDecimal.valueOf(100))
                .divide(average, 0, RoundingMode.HALF_UP)
                .intValueExact();
        Label label = diffPercent <= -THRESHOLD_PERCENT ? Label.BELOW_AVERAGE
                : diffPercent >= THRESHOLD_PERCENT ? Label.ABOVE_AVERAGE
                : Label.AVERAGE;
        return new PriceVerdict(average, diffPercent, label);
    }
}
