package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import kz.nurbolat.bookaggregator.admin.dto.RequeueResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/queues")
@RequiredArgsConstructor
public class AdminQueueController {

    private final AdminQueueService queueService;

    @Operation(summary = "Ready, unacknowledged messages, consumers and ack rate per kr.* queue")
    @GetMapping
    List<QueueStats> queues() {
        return queueService.queues();
    }

    @Operation(summary = "Return up to 500 messages from the dead-letter queue to their original exchange")
    @PostMapping("/dead-letter/requeue")
    RequeueResponse requeue() {
        return queueService.requeueDeadLetters();
    }
}
