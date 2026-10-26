package kz.nurbolat.bookaggregator.favorite;

import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Favorites", description = "Избранные книги пользователя")
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "Список избранного", description = "Избранные книги текущего пользователя.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @GetMapping
    ResponseEntity<List<BookResponse>> getFavorites() {
        return ResponseEntity.ok(favoriteService.getFavorites());
    }

    @Operation(summary = "Добавить в избранное", description = "Повторное добавление не создаёт дубликат.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "404", description = "Книга не найдена")
    @PostMapping("/{bookId}")
    ResponseEntity<Map<String, String>> addFavorite(@PathVariable Long bookId) {
        favoriteService.addFavorite(bookId);
        return ResponseEntity.ok(Map.of("message", "Added to favorites"));
    }

    @Operation(summary = "Убрать из избранного")
    @ApiResponse(responseCode = "204", description = "Книга убрана")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @DeleteMapping("/{bookId}")
    ResponseEntity<Void> removeFavorite(@PathVariable Long bookId) {
        favoriteService.removeFavorite(bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Статус избранного", description = "Показывает, есть ли книга в избранном.")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @GetMapping("/{bookId}/status")
    ResponseEntity<Map<String, Boolean>> isFavorite(@PathVariable Long bookId) {
        return ResponseEntity.ok(Map.of("favorite", favoriteService.isFavorite(bookId)));
    }
}
