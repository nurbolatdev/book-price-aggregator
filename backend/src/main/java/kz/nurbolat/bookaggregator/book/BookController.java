package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.ResourceNotFoundException;
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
        return ResponseEntity.ok(bookSearchService.search(query));
    }

    @GetMapping("/{id}")
    ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));

        return ResponseEntity.ok(BookResponse.from(
                book,
                offerRepository.findByBookIdOrderByPriceAsc(id)
        ));
    }
}
