package kz.nurbolat.bookaggregator.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Письмо из журнала уведомлений")
public record AdminNotificationResponse(
        Long id,
        Instant createdAt,
        @Schema(description = "Когда письмо отправлено; null — ещё нет") Instant sentAt,
        String userEmail,
        @Schema(description = "Тема содержимого: книга для маячка, «Сводка за день» или «Подборка недели»") String title,
        @Schema(description = "INSTANT, DIGEST или WEEKLY") String delivery,
        @Schema(description = "QUEUED, SENT или FAILED") String status,
        @Schema(description = "Причина неудачи; null, если ошибки нет") String error) {
}
