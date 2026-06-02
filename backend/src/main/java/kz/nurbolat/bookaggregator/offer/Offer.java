package kz.nurbolat.bookaggregator.offer;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.book.Book;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    private String currency = "KZT";

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(nullable = false)
    private String url;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt = LocalDateTime.now();
}
