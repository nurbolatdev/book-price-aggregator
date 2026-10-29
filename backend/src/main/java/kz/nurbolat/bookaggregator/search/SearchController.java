package kz.nurbolat.bookaggregator.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.scheduler.RefreshOnDemandService;
import kz.nurbolat.bookaggregator.search.dto.FacetsRequest;
import kz.nurbolat.bookaggregator.search.dto.SearchFacets;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.search.dto.Suggestion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Search", description = "Поиск изданий и подсказки")
@SecurityRequirements
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SuggestionService suggestionService;
    private final SearchFacetsService facetsService;
    private final PopularQueriesService popularQueriesService;
    private final RefreshOnDemandService refreshOnDemandService;

    /** The first page also refreshes stale prices of its editions in the background. */
    @Operation(summary = "Поиск изданий", description = "Ищет по названию, автору или ISBN; без q — каталог. Фильтры по магазинам, цене, переплёту и языку (списки через запятую).")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @GetMapping
    ResponseEntity<PageResponse<SearchItem>> search(@Valid @ParameterObject SearchRequest request) {
        PageResponse<SearchItem> page = searchService.search(request);
        if (request.page() == 0) {
            refreshOnDemandService.requestIfStale(page.content().stream().map(SearchItem::editionId).toList());
        }
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Счётчики фильтров поиска",
            description = "Для каждого магазина, переплёта и языка — сколько изданий вернул бы поиск с теми же фильтрами и только этим значением.")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @GetMapping("/facets")
    ResponseEntity<SearchFacets> facets(@Valid @ParameterObject FacetsRequest request) {
        return ResponseEntity.ok(facetsService.facets(request));
    }

    @Operation(summary = "Подсказки поиска", description = "Варианты для строки ввода от 2 символов.")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @GetMapping("/suggest")
    ResponseEntity<List<Suggestion>> suggest(@RequestParam @NotBlank @Size(min = 2, max = 200) String q) {
        return ResponseEntity.ok(suggestionService.suggest(q));
    }

    @Operation(summary = "Популярные запросы", description = "Топ запросов за 7 дней с результатами; если их мало — дополняется списком из настроек.")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    @GetMapping("/popular")
    ResponseEntity<List<String>> popular(@RequestParam(defaultValue = "6") @Min(1) @Max(12) int limit) {
        return ResponseEntity.ok(popularQueriesService.popular(limit));
    }
}
