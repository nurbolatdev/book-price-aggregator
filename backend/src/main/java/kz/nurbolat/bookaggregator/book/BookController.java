package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookSearchService bookSearchService;
    private final BookService bookService;

    @GetMapping("/search")
    ResponseEntity<List<BookResponse>> search(@RequestParam String query) {
        if (query.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Параметр query не должен быть пустым");
        }
        return ResponseEntity.ok(bookSearchService.search(query));
    }

    @GetMapping("/{id}")
    ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBook(id));
    }
}
