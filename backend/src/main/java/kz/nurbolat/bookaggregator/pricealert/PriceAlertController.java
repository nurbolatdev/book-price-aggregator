package kz.nurbolat.bookaggregator.pricealert;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.pricealert.dto.PriceAlertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    @GetMapping
    ResponseEntity<List<PriceAlert>> getAlerts() {
        return ResponseEntity.ok(priceAlertService.getAlerts());
    }

    @PostMapping("/{bookId}")
    ResponseEntity<PriceAlert> createAlert(@PathVariable Long bookId,
                                           @Valid @RequestBody PriceAlertRequest request) {
        return ResponseEntity.ok(priceAlertService.createAlert(bookId, request.getTargetPrice()));
    }

    @DeleteMapping("/{bookId}")
    ResponseEntity<Map<String, String>> deleteAlert(@PathVariable Long bookId) {
        priceAlertService.deleteAlert(bookId);
        return ResponseEntity.ok(Map.of("message", "Alert removed"));
    }
}
