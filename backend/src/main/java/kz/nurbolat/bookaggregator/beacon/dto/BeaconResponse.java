package kz.nurbolat.bookaggregator.beacon.dto;

import kz.nurbolat.bookaggregator.beacon.BeaconCondition;
import kz.nurbolat.bookaggregator.beacon.BeaconStatus;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A beacon with the edition and its current best price.
 *
 * @param currentBestPrice lowest in-stock price now, {@code null} if no store has it
 */
public record BeaconResponse(
        Long id,
        Long editionId,
        String title,
        String author,
        String coverUrl,
        BeaconCondition conditionType,
        BigDecimal targetPrice,
        Integer targetPercent,
        BeaconStatus status,
        BigDecimal currentBestPrice,
        StoreCode currentBestStore,
        String currentBestUrl,
        BigDecimal baselinePrice,
        Instant lastTriggeredAt,
        Instant createdAt,
        Instant statusChangedAt
) {
}
