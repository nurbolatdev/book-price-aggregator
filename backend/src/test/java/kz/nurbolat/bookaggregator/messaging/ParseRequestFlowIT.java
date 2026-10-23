package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.offer.IngestResult;
import kz.nurbolat.bookaggregator.parser.core.CrawlResult;
import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.StoreCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParseRequestFlowIT extends AbstractListenerIT {

    @Autowired
    private ParseJobPublisher publisher;

    @BeforeEach
    void setUp() {
        when(crawlService.hasParser(any())).thenReturn(true);
    }

    @Test
    void shouldRunQueuedTaskInStoreListener() {
        when(crawlService.crawl(any(), any(), any())).thenReturn(
                new CrawlResult(1L, StoreCode.FLIP, RunStatus.SUCCESS, 1, 0, IngestResult.EMPTY, null));

        assertThat(publisher.publish(StoreCode.FLIP, TriggerType.MANUAL, List.of(11L))).isTrue();

        verify(crawlService, timeout(10_000)).crawl(StoreCode.FLIP, TriggerType.MANUAL, Optional.of(List.of(11L)));
    }

    @Test
    void shouldTryThreeTimesThenDeadLetterWhenCrawlFails() {
        when(crawlService.crawl(any(), any(), any())).thenThrow(new IllegalStateException("database is down"));

        publisher.publish(StoreCode.FLIP, TriggerType.SCHEDULED, List.of());

        Object deadLetter = rabbitTemplate.receiveAndConvert(messagingProperties.deadLetterQueue(), 15_000);
        assertThat(deadLetter).isInstanceOfSatisfying(ParseRequestMessage.class, message -> {
            assertThat(message.store()).isEqualTo(StoreCode.FLIP);
            assertThat(message.trigger()).isEqualTo(TriggerType.SCHEDULED);
        });
        verify(crawlService, times(3)).crawl(any(), any(), any());
    }
}
