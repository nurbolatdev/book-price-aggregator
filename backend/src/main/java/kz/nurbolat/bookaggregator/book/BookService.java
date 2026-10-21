package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.edition.EditionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final EditionService editionService;

    /**
     * Book with its editions and the best price of each edition.
     */
    @Transactional(readOnly = true)
    public BookResponse getBook(Long id) {
        Book book = bookRepository.findWithEditionsById(id)
                .orElseThrow(() -> new NotFoundException("Book not found: " + id));
        return BookResponse.from(book, editionService.findBestPricesOfBook(id));
    }
}
