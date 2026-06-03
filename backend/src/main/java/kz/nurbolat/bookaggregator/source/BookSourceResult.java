package kz.nurbolat.bookaggregator.source;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BookSourceResult {

    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String coverUrl;
    private String description;

    private String source;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String currency;
    private boolean inStock;
    private String url;
}
