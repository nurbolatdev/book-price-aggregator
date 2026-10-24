package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/admin/stores")
@RequiredArgsConstructor
public class AdminStoreController {

    private final AdminParserService parserService;

    @Operation(summary = "Switch a store on or off")
    @PatchMapping("/{storeCode}")
    AdminParserResponse.StoreState toggle(@PathVariable StoreCode storeCode, @Valid @RequestBody StoreToggleRequest request) {
        return parserService.setEnabled(storeCode, request.enabled());
    }
}
