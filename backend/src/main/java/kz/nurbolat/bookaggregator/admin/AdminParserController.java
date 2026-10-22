package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.admin.dto.ParserRunRequest;
import kz.nurbolat.bookaggregator.parser.core.CrawlResult;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin/parsers")
@RequiredArgsConstructor
public class AdminParserController {

    private final StoreCrawlService storeCrawlService;

    /**
     * Runs the store parser synchronously for now; it becomes a queued job returning 202 in M17.
     */
    @Operation(summary = "Run a store parser now (synchronous until queues arrive)")
    @PostMapping("/{storeCode}/run")
    ResponseEntity<CrawlResult> run(@PathVariable StoreCode storeCode,
                                    @Valid @RequestBody(required = false) ParserRunRequest request) {
        Optional<List<Long>> editionIds = Optional.ofNullable(request).map(ParserRunRequest::editionIds);
        return ResponseEntity.ok(storeCrawlService.crawl(storeCode, TriggerType.MANUAL, editionIds));
    }
}
