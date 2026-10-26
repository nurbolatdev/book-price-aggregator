package kz.nurbolat.bookaggregator.store;

import kz.nurbolat.bookaggregator.store.dto.StoreResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Stores", description = "Магазины, которые отслеживает сервис")
@SecurityRequirements
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @Operation(summary = "Список магазинов", description = "Все магазины с признаком, включён ли парсинг.")
    @GetMapping
    ResponseEntity<List<StoreResponse>> getStores() {
        return ResponseEntity.ok(storeService.findAll().stream()
                .map(StoreResponse::from)
                .toList());
    }
}
