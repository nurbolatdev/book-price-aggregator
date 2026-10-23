package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.offer.IngestResult;
import kz.nurbolat.bookaggregator.parser.core.CrawlResult;
import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyIT extends AbstractListenerIT {

    @Autowired
    private MessageSender sender;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void shouldProcessSameMessageOnceWhenDeliveredTwice() throws InterruptedException {
        when(crawlService.hasParser(any())).thenReturn(true);
        when(crawlService.crawl(any(), any(), any())).thenReturn(
                new CrawlResult(1L, StoreCode.FLIP, RunStatus.SUCCESS, 1, 0, IngestResult.EMPTY, null));
        ParseRequestMessage message = new ParseRequestMessage(UUID.randomUUID(), StoreCode.FLIP,
                TriggerType.MANUAL, List.of(), Instant.now());

        sender.send(RabbitTopology.parseRoutingKey(StoreCode.FLIP), message.messageId(), message);
        verify(crawlService, timeout(10_000)).crawl(any(), any(), any());
        sender.send(RabbitTopology.parseRoutingKey(StoreCode.FLIP), message.messageId(), message);
        // The copy is consumed and skipped: the queue empties without a second crawl
        waitUntilQueueIsEmpty();

        verify(crawlService, times(1)).crawl(any(), any(), any());
        assertThat(redis.hasKey("processed:v1:parse:" + message.messageId())).isTrue();
    }

    private void waitUntilQueueIsEmpty() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            var info = rabbitAdmin.getQueueInfo(RabbitTopology.PARSE_FLIP_QUEUE);
            if (info != null && info.getMessageCount() == 0) {
                Thread.sleep(500);
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Queue was not consumed");
    }
}
