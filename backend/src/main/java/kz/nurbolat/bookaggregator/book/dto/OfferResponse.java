package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.offer.Offer;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OfferResponse {

    private Long id;
    private String source;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String currency;
    private boolean inStock;
    private String url;
    private LocalDateTime fetchedAt;

    public static OfferResponse from(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .source(offer.getSource())
                .price(offer.getPrice())
                .originalPrice(offer.getOriginalPrice())
                .currency(offer.getCurrency())
                .inStock(offer.isInStock())
                .url(offer.getUrl())
                .fetchedAt(offer.getFetchedAt())
                .build();
    }
}
