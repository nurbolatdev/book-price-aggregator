package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.book.dto.OfferResponse;
import kz.nurbolat.bookaggregator.common.ResourceNotFoundException;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.offer.PriceHistoryRepository;
import kz.nurbolat.bookaggregator.offer.dto.PriceHistoryResponse;
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
    private final PriceHistoryRepository priceHistoryRepository;

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

    @GetMapping("/{id}/offers")
    ResponseEntity<List<OfferResponse>> getOffers(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found: " + id);
        }
        List<OfferResponse> offers = offerRepository.findByBookIdOrderByPriceAsc(id)
                .stream()
                .map(OfferResponse::from)
                .toList();
        return ResponseEntity.ok(offers);
    }

    @GetMapping("/{id}/price-history")
    ResponseEntity<List<PriceHistoryResponse>> getPriceHistory(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found: " + id);
        }
        List<PriceHistoryResponse> history = offerRepository.findByBookIdOrderByPriceAsc(id)
                .stream()
                .flatMap(offer -> priceHistoryRepository.findByOfferIdOrderByRecordedAtAsc(offer.getId())
                        .stream()
                        .map(PriceHistoryResponse::from))
                .toList();
        return ResponseEntity.ok(history);
    }
}
