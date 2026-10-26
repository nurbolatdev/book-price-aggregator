package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.nurbolat.bookaggregator.admin.dto.AdminHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Health", description = "Здоровье системы")
@RequestMapping("/api/v1/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {

    private final AdminHealthService healthService;

    @Operation(summary = "Здоровье системы", description = "Статус БД, Redis, RabbitMQ, почты и планировщика с метриками нагрузки.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @GetMapping
    AdminHealthResponse health() {
        return healthService.health();
    }
}
