package kz.nurbolat.bookaggregator.favorite;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    ResponseEntity<List<BookResponse>> getFavorites() {
        return ResponseEntity.ok(favoriteService.getFavorites());
    }

    @PostMapping("/{bookId}")
    ResponseEntity<Map<String, String>> addFavorite(@PathVariable Long bookId) {
        favoriteService.addFavorite(bookId);
        return ResponseEntity.ok(Map.of("message", "Added to favorites"));
    }

    @DeleteMapping("/{bookId}")
    ResponseEntity<Void> removeFavorite(@PathVariable Long bookId) {
        favoriteService.removeFavorite(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookId}/status")
    ResponseEntity<Map<String, Boolean>> isFavorite(@PathVariable Long bookId) {
        return ResponseEntity.ok(Map.of("favorite", favoriteService.isFavorite(bookId)));
    }
}
