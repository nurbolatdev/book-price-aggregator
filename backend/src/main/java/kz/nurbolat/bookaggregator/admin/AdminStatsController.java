package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.nurbolat.bookaggregator.admin.dto.AdminActivityDay;
import kz.nurbolat.bookaggregator.admin.dto.AdminStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Admin: Stats", description = "Статистика и активность")
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService statsService;

    @Operation(summary = "KPI за период", description = "Пользователи, маячки, письма, поиски и каталог за период (24h, 7d, 30d).")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @GetMapping("/stats")
    AdminStatsResponse stats(@RequestParam(defaultValue = "24h") String period) {
        return statsService.stats(StatsPeriod.parse(period));
    }

    @Operation(summary = "Активность по дням", description = "Поиски, регистрации, новые маячки и отправленные письма по дням.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @GetMapping("/activity")
    List<AdminActivityDay> activity(@RequestParam(defaultValue = "14") int days) {
        return statsService.activity(days);
    }
}
