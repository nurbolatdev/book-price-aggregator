package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.text.IsbnUtils;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.source.BookSource;
import kz.nurbolat.bookaggregator.source.BookSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Temporary search until the catalog search of M14: looks the query up in Google Books
 * and stores every result as a book with one edition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private static final int MAX_LANGUAGE_LENGTH = 5;

    private final List<BookSource> sources;
    private final BookRepository bookRepository;
    private final EditionRepository editionRepository;

    /**
     * Not cached because it writes to the database.
     */
    @Transactional
    public List<BookResponse> search(String query) {
        // Google Books provides metadata only; offers come from store parsers
        List<BookSourceResult> discovered = sources.stream()
                .map(source -> fetchFromSource(source, query))
                .flatMap(List::stream)
                .toList();

        Set<Book> books = new LinkedHashSet<>();
        for (BookSourceResult result : discovered) {
            books.add(findOrCreateBook(result));
        }

        return books.stream()
                .map(BookResponse::from)
                .toList();
    }

    private List<BookSourceResult> fetchFromSource(BookSource source, String query) {
        log.debug("Fetching from source: {}", source.getName());
        try {
            return source.search(query);
        } catch (Exception e) {
            log.warn("Source {} failed: {}", source.getName(), e.getMessage());
            return List.of();
        }
    }

    private Book findOrCreateBook(BookSourceResult result) {
        String isbn13 = IsbnUtils.normalize(result.getIsbn());
        if (isbn13 != null) {
            var existing = editionRepository.findByIsbn13(isbn13);
            if (existing.isPresent()) {
                return existing.get().getBook();
            }
        }

        Book book = new Book();
        book.setTitle(result.getTitle());
        book.setAuthor(result.getAuthor());
        book.setDescription(result.getDescription());
        book.addEdition(toEdition(result, isbn13));
        return bookRepository.save(book);
    }

    private static Edition toEdition(BookSourceResult result, String isbn13) {
        Edition edition = new Edition();
        edition.setIsbn13(isbn13);
        edition.setPublisher(result.getPublisher());
        edition.setYear(result.getYear() == null ? null : result.getYear().shortValue());
        edition.setPages(result.getPages());
        String language = result.getLanguage();
        edition.setLanguage(language != null && language.length() <= MAX_LANGUAGE_LENGTH ? language : null);
        edition.setCoverUrl(result.getCoverUrl());
        return edition;
    }
}
