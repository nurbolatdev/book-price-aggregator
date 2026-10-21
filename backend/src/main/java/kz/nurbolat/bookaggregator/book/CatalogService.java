package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adds books and editions to the catalog from external metadata, without prices.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final BookRepository bookRepository;
    private final EditionRepository editionRepository;

    /**
     * Returns the existing book when the ISBN (or, without ISBN, the same title and author)
     * is already in the catalog; otherwise stores a new book with one edition.
     */
    @Transactional
    public Book addFromMetadata(BookMetadata metadata) {
        Optional<Book> existing = findExisting(metadata);
        if (existing.isPresent()) {
            return existing.get();
        }

        Book book = new Book();
        book.setTitle(metadata.title());
        book.setAuthor(metadata.author());
        book.setDescription(metadata.description());

        Edition edition = new Edition();
        edition.setIsbn13(metadata.isbn13());
        edition.setPublisher(metadata.publisher());
        edition.setYear(metadata.year() == null ? null : metadata.year().shortValue());
        edition.setPages(metadata.pages());
        edition.setLanguage(metadata.language());
        edition.setCoverUrl(metadata.coverUrl());
        book.addEdition(edition);

        return bookRepository.save(book);
    }

    private Optional<Book> findExisting(BookMetadata metadata) {
        if (metadata.isbn13() != null) {
            return editionRepository.findByIsbn13(metadata.isbn13()).map(Edition::getBook);
        }
        return bookRepository.findFirstByTitleNormAndAuthorNorm(
                TextNormalizer.normalize(metadata.title()),
                TextNormalizer.normalize(metadata.author()));
    }
}
