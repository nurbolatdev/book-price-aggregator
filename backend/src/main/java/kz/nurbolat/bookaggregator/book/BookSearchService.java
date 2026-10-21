package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Temporary search until the catalog search: looks the query up in Google Books
 * and stores every result as a book with one edition.
 */
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final GoogleBooksClient googleBooksClient;
    private final CatalogService catalogService;

    /**
     * Not cached because it writes to the database.
     */
    @Transactional
    public List<BookResponse> search(String query) {
        Set<Book> books = new LinkedHashSet<>();
        for (BookMetadata metadata : googleBooksClient.searchByText(query)) {
            books.add(catalogService.addFromMetadata(metadata));
        }
        return books.stream()
                .map(BookResponse::from)
                .toList();
    }
}
