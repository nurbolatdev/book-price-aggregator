package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import kz.nurbolat.bookaggregator.admin.dto.AdminActivityDay;
import kz.nurbolat.bookaggregator.admin.dto.AdminStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    @Operation(summary = "KPI for the period: users, beacons, emails, searches, catalog")
    @GetMapping("/stats")
    AdminStatsResponse stats(@RequestParam(defaultValue = "24h") String period) {
        return statsService.stats(StatsPeriod.parse(period));
    }

    @Operation(summary = "Daily searches, registrations, new beacons and sent emails")
    @GetMapping("/activity")
    List<AdminActivityDay> activity(@RequestParam(defaultValue = "14") int days) {
        return statsService.activity(days);
    }
}
