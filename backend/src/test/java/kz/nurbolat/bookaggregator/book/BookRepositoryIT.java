package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.book;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class BookRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldStoreNormalizedTitleAndAuthorWhenBookIsCreated() {
        Book book = bookRepository.saveAndFlush(book("«Ёлка» и другие", "Чуковский, Корней"));

        assertThat(normalized(book.getId())).containsExactly("елка и другие", "чуковский корней");
    }

    @Test
    void shouldRenormalizeWhenTitleChanges() {
        Book book = bookRepository.saveAndFlush(book("Старое название", null));

        book.setTitle("Новое: название!");
        bookRepository.saveAndFlush(book);

        assertThat(normalized(book.getId())).containsExactly("новое название", null);
    }

    private String[] normalized(Long bookId) {
        return jdbcTemplate.queryForObject(
                "SELECT title_norm, author_norm FROM books WHERE id = ?",
                (rs, row) -> new String[]{rs.getString(1), rs.getString(2)},
                bookId);
    }
}
