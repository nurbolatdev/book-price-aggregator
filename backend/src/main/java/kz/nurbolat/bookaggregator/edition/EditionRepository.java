package kz.nurbolat.bookaggregator.edition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {

    Optional<Edition> findByIsbn13(String isbn13);

    List<Edition> findByBookId(Long bookId);
}
