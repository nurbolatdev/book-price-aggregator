package kz.nurbolat.bookaggregator.home;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.home.dto.DropItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Home", description = "Данные главной страницы")
@SecurityRequirements
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final DropsService dropsService;

    @Operation(summary = "Недавние снижения цен", description = "Издания, цена которых недавно упала.")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @GetMapping("/drops")
    ResponseEntity<List<DropItem>> drops(@RequestParam(defaultValue = "8") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(dropsService.getDrops(limit));
    }
}
