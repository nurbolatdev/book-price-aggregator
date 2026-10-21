package kz.nurbolat.bookaggregator.edition;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lowest price among the offers of an edition at the end of a day (UTC).
 */
public record DailyPrice(LocalDate date, BigDecimal price) {
}
