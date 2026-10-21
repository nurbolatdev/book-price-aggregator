package kz.nurbolat.bookaggregator.edition.dto;

import kz.nurbolat.bookaggregator.edition.PricePeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lowest price of an edition per day, with min / avg / max over the period.
 * All aggregates are {@code null} when there is no history.
 */
public record PriceHistoryResponse(
        Long editionId,
        PricePeriod period,
        List<Point> points,
        BigDecimal min,
        BigDecimal avg,
        BigDecimal max
) {

    public record Point(LocalDate date, BigDecimal price) {
    }
}
