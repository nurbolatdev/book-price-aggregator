package kz.nurbolat.bookaggregator.edition;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {

    Optional<Edition> findByIsbn13(String isbn13);

    @Query("SELECT e FROM Edition e JOIN FETCH e.book WHERE e.id = :id")
    Optional<Edition> findWithBookById(Long id);

    List<Edition> findByBookId(Long bookId);
}
