package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.admin.dto.AdminParserResponse;
import kz.nurbolat.bookaggregator.admin.dto.StoreToggleRequest;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Stores", description = "Переключатель магазинов")
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final AdminParserService parserService;

    @Operation(summary = "Включить или выключить магазин", description = "Включает или выключает магазин для обходов.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PatchMapping("/{storeCode}")
    AdminParserResponse.StoreState toggle(@PathVariable StoreCode storeCode, @Valid @RequestBody StoreToggleRequest request) {
        return parserService.setEnabled(storeCode, request.enabled());
    }
}
