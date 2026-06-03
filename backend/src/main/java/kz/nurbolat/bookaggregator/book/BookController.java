package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookSearchService bookSearchService;
    private final BookRepository bookRepository;
    private final OfferRepository offerRepository;

    @GetMapping("/search")
    ResponseEntity<List<BookResponse>> search(@RequestParam String query) {
        if (query.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<Book> books = bookSearchService.search(query);

        List<BookResponse> response = books.stream()
                .map(book -> BookResponse.from(
                        book,
                        offerRepository.findByBookIdOrderByPriceAsc(book.getId())
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .map(book -> BookResponse.from(
                        book,
                        offerRepository.findByBookIdOrderByPriceAsc(id)
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
