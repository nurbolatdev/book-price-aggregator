package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.source.BookSource;
import kz.nurbolat.bookaggregator.source.BookSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final List<BookSource> sources;
    private final BookRepository bookRepository;
    private final OfferRepository offerRepository;

    @Cacheable(value = "book-search", key = "#query.toLowerCase().trim()")
    @Transactional
    public List<BookResponse> search(String query) {
        log.debug("Cache miss — fetching from sources for query: {}", query);

        // Phase 1: discovery sources (Google Books) find real books
        List<CompletableFuture<List<BookSourceResult>>> futures = sources.stream()
                .filter(BookSource::isDiscoverySource)
                .map(source -> fetchAsync(source, query))
                .toList();

        List<BookSourceResult> discovered = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        if (discovered.isEmpty()) {
            return List.of();
        }

        List<Book> books = saveResults(discovered);

        // Phase 2: price sources (Kaspi, Ozon) enrich each real book with offers
        List<BookSource> priceSources = sources.stream()
                .filter(s -> !s.isDiscoverySource())
                .toList();

        for (BookSource source : priceSources) {
            for (Book book : books) {
                source.enrichBook(book.getTitle(), book.getAuthor(), book.getIsbn())
                        .forEach(result -> saveOffer(book, result));
            }
        }

        return books.stream()
                .map(book -> BookResponse.from(
                        book,
                        offerRepository.findByBookIdOrderByPriceAsc(book.getId())
                ))
                .toList();
    }

    @Async
    public CompletableFuture<List<BookSourceResult>> fetchAsync(BookSource source, String query) {
        log.debug("Fetching from source: {}", source.getName());
        try {
            return CompletableFuture.completedFuture(source.search(query));
        } catch (Exception e) {
            log.error("Source {} failed: {}", source.getName(), e.getMessage());
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private List<Book> saveResults(List<BookSourceResult> results) {
        List<Book> books = new ArrayList<>();

        for (BookSourceResult result : results) {
            Book book = findOrCreateBook(result);
            saveOffer(book, result);
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

    private void saveOffer(Book book, BookSourceResult result) {
        List<Offer> existing = offerRepository.findByBookIdAndSource(book.getId(), result.getSource());

        Offer offer = existing.isEmpty() ? new Offer() : existing.get(0);
        offer.setBook(book);
        offer.setSource(result.getSource());
        offer.setPrice(result.getPrice());
        offer.setOriginalPrice(result.getOriginalPrice());
        offer.setCurrency(result.getCurrency());
        offer.setInStock(result.isInStock());
        offer.setUrl(result.getUrl());
        offer.setFetchedAt(LocalDateTime.now());

        offerRepository.save(offer);
    }
}
