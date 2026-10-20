package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.source.BookSource;
import kz.nurbolat.bookaggregator.source.BookSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final List<BookSource> sources;
    private final BookRepository bookRepository;
    private final OfferRepository offerRepository;

    /**
     * Looks the query up in metadata sources and stores new books.
     * Not cached because it writes to the database; replaced by catalog search in M14.
     */
    @Transactional
    public List<BookResponse> search(String query) {
        // Google Books provides metadata only; offers come from store parsers
        List<BookSourceResult> discovered = sources.stream()
                .map(source -> fetchFromSource(source, query))
                .flatMap(List::stream)
                .toList();

        if (discovered.isEmpty()) {
            return List.of();
        }

        List<Book> books = saveResults(discovered);

        return books.stream()
                .map(book -> BookResponse.from(
                        book,
                        offerRepository.findByBookIdOrderByPriceAsc(book.getId())
                ))
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

    private List<Book> saveResults(List<BookSourceResult> results) {
        List<Book> books = new ArrayList<>();

        for (BookSourceResult result : results) {
            Book book = findOrCreateBook(result);
            if (!books.contains(book)) {
                books.add(book);
            }
        }

        return books;
    }

    private Book findOrCreateBook(BookSourceResult result) {
        if (result.getIsbn() != null) {
            Optional<Book> existing = bookRepository.findByIsbn(result.getIsbn());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Book book = new Book();
        book.setTitle(result.getTitle());
        book.setAuthor(result.getAuthor());
        book.setIsbn(result.getIsbn());
        book.setPublisher(result.getPublisher());
        book.setCoverUrl(result.getCoverUrl());
        book.setDescription(result.getDescription());
        return bookRepository.save(book);
    }
}
