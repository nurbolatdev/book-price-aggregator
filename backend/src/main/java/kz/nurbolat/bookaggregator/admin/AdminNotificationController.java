package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.admin.dto.AdminNotificationResponse;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.notification.NotificationDelivery;
import kz.nurbolat.bookaggregator.notification.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@Tag(name = "Admin: Notifications", description = "Журнал писем и повторная отправка")
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService service;

    @Operation(summary = "Журнал уведомлений", description = "Письма пользователям, новые сверху; фильтры по статусу и типу доставки.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @GetMapping
    PageResponse<AdminNotificationResponse> list(
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationDelivery delivery,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Не меньше 0") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "Не меньше 1")
            @Max(value = 100, message = "Не больше 100") int size) {
        return service.list(status, delivery, page, size);
    }

    @Operation(summary = "Повторить отправку",
            description = "Возвращает неудачное (FAILED) уведомление в очередь; доставка подхватит его сама.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "404", description = "Уведомление не найдено")
    @ApiResponse(responseCode = "409", description = "Уведомление не в статусе FAILED или email пользователя не подтверждён")
    @PostMapping("/{id}/retry")
    AdminNotificationResponse retry(@PathVariable Long id) {
        return service.retry(id);
    }
}
