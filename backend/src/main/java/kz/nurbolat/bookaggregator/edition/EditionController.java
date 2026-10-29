package kz.nurbolat.bookaggregator.edition;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.edition.dto.PriceHistoryResponse;
import kz.nurbolat.bookaggregator.edition.dto.SimilarEdition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.scheduler.RefreshOnDemandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Editions", description = "Карточка издания: цены по магазинам и история цены")
@SecurityRequirements
@RequestMapping("/api/v1/editions")
@RequiredArgsConstructor
public class EditionController {

    private final EditionService editionService;
    private final PriceHistoryService priceHistoryService;
    private final RefreshOnDemandService refreshOnDemandService;
    private final SimilarEditionsService similarEditionsService;

    /** Answers from stored prices; stale ones are refreshed in the background. */
    @Operation(summary = "Карточка издания", description = "Отдаёт сохранённые цены; устаревшие обновляются в фоне.")
    @ApiResponse(responseCode = "404", description = "Издание не найдено")
    @GetMapping("/{id}")
    ResponseEntity<EditionDetailResponse> getDetail(@PathVariable Long id) {
        refreshOnDemandService.requestIfStale(List.of(id));
        return ResponseEntity.ok(editionService.getDetail(id));
    }

    @Operation(summary = "История цены", description = "Минимальная цена по дням за период 1M, 3M или 6M.")
    @ApiResponse(responseCode = "404", description = "Издание не найдено")
    @GetMapping("/{id}/price-history")
    ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable Long id,
            @Parameter(description = "1M, 3M или 6M", schema = @Schema(defaultValue = "3M"))
            @RequestParam(defaultValue = "3M") PricePeriod period) {
        return ResponseEntity.ok(priceHistoryService.getHistory(id, period));
    }

    @Operation(summary = "Похожие книги",
            description = "Сначала другие книги того же автора, затем книги на том же языке; учитываются только цены в наличии.")
    @ApiResponse(responseCode = "404", description = "Издание не найдено")
    @GetMapping("/{id}/similar")
    ResponseEntity<List<SimilarEdition>> getSimilar(
            @PathVariable Long id,
            @Parameter(description = "Сколько книг вернуть, 1–12", schema = @Schema(defaultValue = "4"))
            @RequestParam(defaultValue = "4") @Min(1) @Max(12) int limit) {
        return ResponseEntity.ok(similarEditionsService.getSimilar(id, limit));
    }
}
