package kz.nurbolat.bookaggregator.book.dto;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.offer.Offer;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private String coverUrl;
    private String description;
    private List<OfferResponse> offers;
    private BigDecimal bestPrice;

    public static BookResponse from(Book book, List<Offer> offers) {
        BigDecimal bestPrice = offers.stream()
                .filter(Offer::isInStock)
                .map(Offer::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .coverUrl(book.getCoverUrl())
                .description(book.getDescription())
                .offers(offers.stream().map(OfferResponse::from).toList())
                .bestPrice(bestPrice)
                .build();
    }
}
