package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse;
import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final EditionRepository editionRepository;
    private final EditionPriceRepository editionPriceRepository;
    private final Clock clock;

    /**
     * Daily lowest price over the period (days without changes keep the previous price)
     * and its min / avg / max.
     */
    @Transactional(readOnly = true)
    public PriceHistoryResponse getHistory(Long editionId, PricePeriod period) {
        if (!editionRepository.existsById(editionId)) {
            throw new NotFoundException("Edition not found: " + editionId);
        }
        LocalDate today = LocalDate.now(clock);
        List<Point> points = editionPriceRepository
                .findDailyMinimums(editionId, today.minusDays(period.getDays()), today).stream()
                .map(daily -> new Point(daily.date(), daily.price()))
                .toList();
        if (points.isEmpty()) {
            return new PriceHistoryResponse(editionId, period, points, null, null, null);
        }

        BigDecimal min = points.stream().map(Point::price).min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal max = points.stream().map(Point::price).max(Comparator.naturalOrder()).orElseThrow();
        BigDecimal avg = points.stream()
                .map(Point::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(points.size()), 2, RoundingMode.HALF_UP);
        return new PriceHistoryResponse(editionId, period, points, min, avg, max);
    }
}
