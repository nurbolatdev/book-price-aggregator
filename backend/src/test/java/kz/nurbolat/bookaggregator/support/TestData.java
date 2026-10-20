package kz.nurbolat.bookaggregator.support;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.user.User;

import java.util.UUID;

/**
 * Builders for entities used across integration tests.
 */
public final class TestData {

    private TestData() {
    }

    public static Book book(String title, String author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    public static Edition edition(String isbn13) {
        Edition edition = new Edition();
        edition.setIsbn13(isbn13);
        return edition;
    }

    public static User user() {
        User user = new User();
        user.setEmail("user-" + UUID.randomUUID() + "@example.com");
        user.setPassword("x");
        return user;
    }
}
