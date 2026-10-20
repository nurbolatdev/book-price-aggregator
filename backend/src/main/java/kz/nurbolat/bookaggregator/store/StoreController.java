package kz.nurbolat.bookaggregator.store;

import kz.nurbolat.bookaggregator.store.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    ResponseEntity<List<StoreResponse>> getStores() {
        return ResponseEntity.ok(storeService.findAll().stream()
                .map(StoreResponse::from)
                .toList());
    }
}
