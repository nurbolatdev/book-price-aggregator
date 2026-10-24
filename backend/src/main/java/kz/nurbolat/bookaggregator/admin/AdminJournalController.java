package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.admin.dto.AdminEventResponse;
import kz.nurbolat.bookaggregator.admin.dto.AdminUserResponse;
import kz.nurbolat.bookaggregator.admin.dto.SearchQueryStat;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminJournalController {

    private final AdminJournalService journalService;

    @Operation(summary = "System event journal, newest first")
    @GetMapping("/events")
    PageResponse<AdminEventResponse> events(
            @RequestParam(required = false) SystemEvent.Level level,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return journalService.events(level, page, size);
    }

    @Operation(summary = "Top 20 search queries of the period")
    @GetMapping("/search/top")
    List<SearchQueryStat> topQueries(@RequestParam(defaultValue = "24h") String period) {
        return journalService.topQueries(StatsPeriod.parse(period));
    }

    @Operation(summary = "Most frequent search queries without results")
    @GetMapping("/search/zero-results")
    List<SearchQueryStat> zeroResults(@RequestParam(defaultValue = "7d") String period) {
        return journalService.zeroResultQueries(StatsPeriod.parse(period));
    }

    @Operation(summary = "Users with registration date, email confirmation and beacon count")
    @GetMapping("/users")
    PageResponse<AdminUserResponse> users(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return journalService.users(page, size);
    }
}
