package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.admin.dto.QueueStats;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminQueueServiceTest {

    private static final Instant T0 = Instant.parse("2026-03-10T10:00:00Z");

    private final RabbitAdminClient client = mock(RabbitAdminClient.class);
    private final Clock clock = mock(Clock.class);
    private final AdminQueueService service = new AdminQueueService(client,
            new RabbitAdminProperties("http://localhost:15672", "guest", "guest", "/", Duration.ofSeconds(3),
                    Duration.ofSeconds(15)),
            mock(RabbitTemplate.class), null, mock(SystemEventService.class), clock);

    @Test
    void shouldReuseQueueFiguresWithinTheCacheTtl() {
        List<QueueStats> first = List.of(new QueueStats("parse.flip", 5, 0, 1, 0.5));
        List<QueueStats> second = List.of(new QueueStats("parse.flip", 9, 0, 1, 0.5));
        when(clock.instant()).thenReturn(T0, T0.plusSeconds(14), T0.plusSeconds(16));
        when(client.queues()).thenReturn(first, second);

        assertThat(service.queues()).isSameAs(first);
        assertThat(service.queues()).isSameAs(first);
        verify(client, times(1)).queues();

        assertThat(service.queues()).isSameAs(second);
        verify(client, times(2)).queues();
    }
}
