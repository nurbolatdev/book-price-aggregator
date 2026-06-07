package kz.nurbolat.bookaggregator.offer.dto;

import kz.nurbolat.bookaggregator.offer.PriceHistory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PriceHistoryResponse {

    private String source;
    private BigDecimal price;
    private LocalDateTime recordedAt;

    public static PriceHistoryResponse from(PriceHistory history) {
        return PriceHistoryResponse.builder()
                .source(history.getOffer().getSource())
                .price(history.getPrice())
                .recordedAt(history.getRecordedAt())
                .build();
    }
}
