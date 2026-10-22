package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adds books and editions to the catalog from metadata (Google Books or a store page), without prices.
 */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final BookRepository bookRepository;
    private final EditionRepository editionRepository;

    /**
     * Book of {@link #findOrCreateEdition(BookMetadata)}.
     */
    @Transactional
    public Book addFromMetadata(BookMetadata metadata) {
        return findOrCreateEdition(metadata).getBook();
    }

    /**
     * Edition with the ISBN if it exists. Otherwise a book with the same normalized title and
     * author gets a new edition (with an ISBN) or is returned as is (without one, its editions
     * cannot be told apart); if there is no such book, a new book with one edition is stored.
     */
    @Transactional
    public Edition findOrCreateEdition(BookMetadata metadata) {
        if (metadata.isbn13() != null) {
            Optional<Edition> existing = editionRepository.findByIsbn13(metadata.isbn13());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Optional<Book> sameWork = bookRepository.findFirstByTitleNormAndAuthorNorm(
                TextNormalizer.normalize(metadata.title()),
                TextNormalizer.normalize(metadata.author()));
        if (sameWork.isPresent() && metadata.isbn13() == null && !sameWork.get().getEditions().isEmpty()) {
            return sameWork.get().getEditions().get(0);
        }

        Edition edition = newEdition(metadata);
        if (sameWork.isPresent()) {
            // Persist the edition itself: saving the managed book would merge a copy of it
            sameWork.get().addEdition(edition);
            return editionRepository.save(edition);
        }
        Book book = newBook(metadata);
        book.addEdition(edition);
        bookRepository.save(book);
        return edition;
    }

    @Transactional(readOnly = true)
    public Optional<Edition> findEditionByIsbn(String isbn13) {
        return editionRepository.findByIsbn13(isbn13);
    }

    /**
     * Editions of books whose normalized title and author equal the given ones exactly.
     */
    @Transactional(readOnly = true)
    public List<Edition> findEditionsByExactTitleAndAuthor(String title, String author) {
        return editionRepository.findByBookTitleNormAndAuthorNorm(
                TextNormalizer.normalize(title), TextNormalizer.normalize(author));
    }

    private static Book newBook(BookMetadata metadata) {
        Book book = new Book();
        book.setTitle(metadata.title());
        book.setAuthor(metadata.author());
        book.setDescription(metadata.description());
        return book;
    }

    private static Edition newEdition(BookMetadata metadata) {
        Edition edition = new Edition();
        edition.setIsbn13(metadata.isbn13());
        edition.setPublisher(metadata.publisher());
        edition.setYear(metadata.year() == null ? null : metadata.year().shortValue());
        edition.setPages(metadata.pages());
        edition.setLanguage(metadata.language());
        edition.setCoverUrl(metadata.coverUrl());
        if (metadata.binding() != null) {
            edition.setBinding(metadata.binding());
        }
        return edition;
    }
}
