package kz.nurbolat.bookaggregator.book;

import jakarta.persistence.*;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Edition;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A work (title + author). Concrete printings are {@link Edition}s.
 */
@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "title_norm", nullable = false)
    private String titleNorm;

    @Column(name = "author_norm")
    private String authorNorm;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Edition> editions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void addEdition(Edition edition) {
        edition.setBook(this);
        editions.add(edition);
    }

    @PrePersist
    private void onCreate() {
        normalize();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        this.titleNorm = TextNormalizer.normalize(title);
        this.authorNorm = TextNormalizer.normalize(author);
    }
}
