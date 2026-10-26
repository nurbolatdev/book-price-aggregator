package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import kz.nurbolat.bookaggregator.admin.dto.RequeueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Admin: Queues", description = "Очереди RabbitMQ")
@RequestMapping("/api/v1/admin/queues")
@RequiredArgsConstructor
public class AdminQueueController {

    private final AdminQueueService queueService;

    @Operation(summary = "Состояние очередей", description = "Готовые и неподтверждённые сообщения, потребители и скорость подтверждений по очередям kr.*.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @GetMapping
    List<QueueStats> queues() {
        return queueService.queues();
    }

    @Operation(summary = "Вернуть из DLQ", description = "Возвращает до 500 сообщений из очереди недоставленных в исходный обменник.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @PostMapping("/dead-letter/requeue")
    RequeueResponse requeue() {
        return queueService.requeueDeadLetters();
    }
}
