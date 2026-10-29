package kz.nurbolat.bookaggregator.beacon;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.beacon.dto.BeaconResponse;
import kz.nurbolat.bookaggregator.beacon.dto.CreateBeaconRequest;
import kz.nurbolat.bookaggregator.beacon.dto.UnsubscribeResponse;
import kz.nurbolat.bookaggregator.beacon.dto.UpdateBeaconRequest;
import kz.nurbolat.bookaggregator.user.dto.TokenRequest;
import kz.nurbolat.bookaggregator.scheduler.RefreshOnDemandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Beacons", description = "Маячки: письмо, когда цена издания достигнет нужного уровня")
@RequestMapping("/api/v1/beacons")
@RequiredArgsConstructor
public class BeaconController {

    private final BeaconService beaconService;
    private final RefreshOnDemandService refreshOnDemandService;

    @Operation(summary = "Список маячков", description = "Маячки текущего пользователя, можно отфильтровать по статусу и изданию (0 или 1 маячок).")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @GetMapping
    List<BeaconResponse> list(@RequestParam(required = false) BeaconStatus status,
                              @RequestParam(required = false) Long editionId) {
        return beaconService.list(status, editionId);
    }

    /** The edition's prices are refreshed in the background if they are stale, so the beacon starts from fresh data. */
    @Operation(summary = "Создать маячок", description = "Один маячок на издание; цены издания при необходимости обновляются в фоне.")
    @ApiResponse(responseCode = "201", description = "Маячок создан")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "404", description = "Издание не найдено")
    @ApiResponse(responseCode = "409", description = "На это издание уже есть маячок")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping
    ResponseEntity<BeaconResponse> create(@Valid @RequestBody CreateBeaconRequest request) {
        BeaconResponse beacon = beaconService.create(request);
        refreshOnDemandService.requestIfStale(List.of(beacon.editionId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(beacon);
    }

    @Operation(summary = "Изменить маячок", description = "Меняет условие и/или статус (ACTIVE или PAUSED).")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "404", description = "Маячок не найден")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PatchMapping("/{id}")
    BeaconResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBeaconRequest request) {
        return beaconService.update(id, request);
    }

    @Operation(summary = "Удалить маячок")
    @ApiResponse(responseCode = "204", description = "Маячок удалён")
    @ApiResponse(responseCode = "401", description = "Требуется авторизация")
    @ApiResponse(responseCode = "404", description = "Маячок не найден")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        beaconService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Link from a beacon email: pauses the beacon without signing in. */
    @Operation(summary = "Отписка по ссылке из письма", description = "Ставит маячок на паузу по токену, без входа.")
    @ApiResponse(responseCode = "400", description = "Токен недействителен")
    @ApiResponse(responseCode = "401", description = "Срок действия токена истёк")
    @SecurityRequirements
    @PostMapping("/unsubscribe")
    UnsubscribeResponse unsubscribe(@Valid @RequestBody TokenRequest request) {
        return beaconService.unsubscribe(request.token());
    }
}
