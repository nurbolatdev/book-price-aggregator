package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import kz.nurbolat.bookaggregator.admin.dto.AdminHealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/health")
@RequiredArgsConstructor
public class AdminHealthController {

    private final AdminHealthService healthService;

    @Operation(summary = "Status of db, redis, rabbit, mail and scheduler with load metrics")
    @GetMapping
    AdminHealthResponse health() {
        return healthService.health();
    }
}
