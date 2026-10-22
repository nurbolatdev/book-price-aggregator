package kz.nurbolat.bookaggregator.edition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {

    Optional<Edition> findByIsbn13(String isbn13);

    @Query("SELECT e FROM Edition e JOIN FETCH e.book WHERE e.id = :id")
    Optional<Edition> findWithBookById(Long id);

    @Query("""
            SELECT e FROM Edition e JOIN FETCH e.book b
            WHERE b.titleNorm = :titleNorm AND b.authorNorm = :authorNorm
            ORDER BY e.id
            """)
    List<Edition> findByBookTitleNormAndAuthorNorm(String titleNorm, String authorNorm);

    List<Edition> findByBookId(Long bookId);
}
