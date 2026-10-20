package kz.nurbolat.bookaggregator.edition;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.book.Book;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A concrete printing of a book (ISBN, publisher, binding). Prices and beacons live on this level.
 */
@Entity
@Table(name = "editions")
@Getter
@Setter
@NoArgsConstructor
public class Edition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(length = 13, unique = true)
    private String isbn13;

    private String publisher;

    private Short year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Binding binding = Binding.UNKNOWN;

    @Column(length = 5)
    private String language;

    private Integer pages;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
