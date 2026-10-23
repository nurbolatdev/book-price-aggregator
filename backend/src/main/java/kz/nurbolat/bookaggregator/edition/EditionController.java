package kz.nurbolat.bookaggregator.edition;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse;
import kz.nurbolat.bookaggregator.scheduler.RefreshOnDemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/editions")
@RequiredArgsConstructor
public class EditionController {

    private final EditionService editionService;
    private final PriceHistoryService priceHistoryService;
    private final RefreshOnDemandService refreshOnDemandService;

    /** Answers from stored prices; stale ones are refreshed in the background. */
    @GetMapping("/{id}")
    ResponseEntity<EditionDetailResponse> getDetail(@PathVariable Long id) {
        refreshOnDemandService.requestIfStale(List.of(id));
        return ResponseEntity.ok(editionService.getDetail(id));
    }

    @GetMapping("/{id}/price-history")
    ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable Long id,
            @Parameter(description = "1M, 3M или 6M", schema = @Schema(defaultValue = "3M"))
            @RequestParam(defaultValue = "3M") PricePeriod period) {
        return ResponseEntity.ok(priceHistoryService.getHistory(id, period));
    }
}
