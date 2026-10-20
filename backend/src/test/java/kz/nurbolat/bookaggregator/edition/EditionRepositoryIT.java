package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.BookRepository;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.book;
import static kz.nurbolat.bookaggregator.support.TestData.edition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class EditionRepositoryIT extends AbstractIntegrationTest {

    private static final String ISBN = "9780306406157";

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EditionRepository editionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldFindEditionByIsbnWhenSavedThroughBook() {
        Book book = book("Мастер и Маргарита", "Булгаков");
        book.addEdition(edition(ISBN));
        bookRepository.saveAndFlush(book);

        assertThat(editionRepository.findByIsbn13(ISBN))
                .hasValueSatisfying(found -> {
                    assertThat(found.getBook().getId()).isEqualTo(book.getId());
                    assertThat(found.getBinding()).isEqualTo(Binding.UNKNOWN);
                    assertThat(found.getCreatedAt()).isNotNull();
                });
    }

    @Test
    void shouldRejectSecondEditionWhenIsbnIsAlreadyUsed() {
        Book first = book("Мастер и Маргарита", "Булгаков");
        first.addEdition(edition(ISBN));
        bookRepository.saveAndFlush(first);

        Book second = book("Другая книга", null);
        second.addEdition(edition(ISBN));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldDeleteEditionsWhenBookIsDeletedInDatabase() {
        Book book = book("Война и мир", "Толстой");
        book.addEdition(edition(null));
        book.addEdition(edition(null));
        bookRepository.saveAndFlush(book);
        assertThat(editionRepository.findByBookId(book.getId())).hasSize(2);

        jdbcTemplate.update("DELETE FROM books WHERE id = ?", book.getId());

        Integer left = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM editions WHERE book_id = ?", Integer.class, book.getId());
        assertThat(left).isZero();
    }
}
