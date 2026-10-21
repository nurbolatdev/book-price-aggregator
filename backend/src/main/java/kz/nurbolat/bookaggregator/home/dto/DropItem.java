package kz.nurbolat.bookaggregator.home.dto;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;

/**
 * Edition whose best in-stock price fell during the last week.
 *
 * @param previousPrice best price seven days ago
 * @param dropPercent   rounded drop relative to the previous price
 */
public record DropItem(
        Long editionId,
        Long bookId,
        String title,
        String author,
        String coverUrl,
        StoreCode store,
        BigDecimal price,
        BigDecimal previousPrice,
        int dropPercent
) {
}
