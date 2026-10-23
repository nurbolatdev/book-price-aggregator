package kz.nurbolat.bookaggregator.messaging;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import kz.nurbolat.bookaggregator.messaging.message.ParseRequestMessage;
import kz.nurbolat.bookaggregator.parser.core.CrawlResult;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static kz.nurbolat.bookaggregator.messaging.RabbitTopology.*;

/**
 * Runs crawl tasks: one listener per store queue with a single consumer, so a store is crawled by one task
 * at a time and a slow store never holds up the others. A task that cannot run (no parser, store switched
 * off or paused by its circuit breaker) is acknowledged and logged — retrying would not help. A crawl that
 * throws is retried and finally dead-lettered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ParseRequestListener {

    private static final Set<CircuitBreaker.State> PAUSED = Set.of(
            CircuitBreaker.State.OPEN, CircuitBreaker.State.FORCED_OPEN);

    private final StoreCrawlService crawlService;
    private final StoreService storeService;
    private final CircuitBreakerRegistry circuitBreakers;
    private final ProcessedMessages processedMessages;

    @RabbitListener(id = "parse-flip", queues = PARSE_FLIP_QUEUE, concurrency = "1")
    void onFlip(ParseRequestMessage message) {
        handle(message);
    }

    @RabbitListener(id = "parse-kaspi", queues = PARSE_KASPI_QUEUE, concurrency = "1")
    void onKaspi(ParseRequestMessage message) {
        handle(message);
    }

    @RabbitListener(id = "parse-meloman", queues = PARSE_MELOMAN_QUEUE, concurrency = "1")
    void onMeloman(ParseRequestMessage message) {
        handle(message);
    }

    @RabbitListener(id = "parse-wildberries", queues = PARSE_WILDBERRIES_QUEUE, concurrency = "1")
    void onWildberries(ParseRequestMessage message) {
        handle(message);
    }

    @RabbitListener(id = "parse-ozon", queues = PARSE_OZON_QUEUE, concurrency = "1")
    void onOzon(ParseRequestMessage message) {
        handle(message);
    }

    void handle(ParseRequestMessage message) {
        MDC.put("traceId", message.messageId().toString());
        try {
            if (!processedMessages.runOnce(ProcessedMessages.PARSE, message.messageId(), () -> crawl(message))) {
                log.info("Parse request {} already processed, skipped", message.messageId());
            }
        } finally {
            MDC.remove("traceId");
        }
    }

    private void crawl(ParseRequestMessage message) {
        StoreCode store = message.store();
        String skipped = skipReason(store);
        if (skipped != null) {
            log.info("Parse request {} for {} skipped: {}", message.messageId(), store, skipped);
            return;
        }
        CrawlResult result = crawlService.crawl(store, message.trigger(),
                message.editionIds().isEmpty() ? Optional.empty() : Optional.of(message.editionIds()));
        log.info("Parse request {} for {} done: run={} status={}", message.messageId(), store, result.runId(),
                result.status());
    }

    private String skipReason(StoreCode store) {
        if (!crawlService.hasParser(store)) {
            return "no parser";
        }
        if (!storeService.getByCode(store).isEnabled()) {
            return "store is switched off";
        }
        boolean paused = circuitBreakers.find("store-" + store)
                .map(breaker -> PAUSED.contains(breaker.getState()))
                .orElse(false);
        return paused ? "store is paused by its circuit breaker" : null;
    }
}
