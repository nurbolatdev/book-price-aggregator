package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.admin.dto.AdminParserResponse;
import kz.nurbolat.bookaggregator.admin.dto.ParserRunQueuedResponse;
import kz.nurbolat.bookaggregator.admin.dto.ParserRunRequest;
import kz.nurbolat.bookaggregator.scheduler.ManualRunService;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Admin: Parsers", description = "Парсеры магазинов")
@RequestMapping("/api/v1/admin/parsers")
@RequiredArgsConstructor
public class AdminParserController {

    private final ManualRunService manualRunService;
    private final AdminParserService parserService;

    @Operation(summary = "Состояние парсеров", description = "Последний запуск, доля успехов и circuit breaker по каждому магазину.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @GetMapping
    List<AdminParserResponse> parsers() {
        return parserService.parsers();
    }

    /**
     * Queues a crawl of the store; the store's queue runs it, results appear in parser_runs.
     */
    @Operation(summary = "Запустить обход магазина", description = "Ставит обход магазина в очередь; результаты появляются в parser_runs.")
    @ApiResponse(responseCode = "202", description = "Обход поставлен в очередь")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping("/{storeCode}/run")
    ResponseEntity<ParserRunQueuedResponse> run(@PathVariable StoreCode storeCode,
                                                @Valid @RequestBody(required = false) ParserRunRequest request) {
        List<Long> editionIds = request == null || request.editionIds() == null ? List.of() : request.editionIds();
        manualRunService.queue(storeCode, editionIds);
        return ResponseEntity.accepted().body(new ParserRunQueuedResponse(true));
    }
}
