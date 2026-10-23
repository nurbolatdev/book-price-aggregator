package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.edition.EditionCacheService;
import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import kz.nurbolat.bookaggregator.store.StoreCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EditionCacheEvictListenerTest {

    private final EditionCacheService cache = mock(EditionCacheService.class);
    private final ProcessedMessages processed = spy(new ProcessedMessages(null));
    private final EditionCacheEvictListener listener = new EditionCacheEvictListener(cache, processed);

    @Test
    void shouldEvictCardOfEditionWhosePriceChanged() {
        doReturn(true).when(processed).claim(any());

        listener.onPriceChanged(message());

        verify(cache).evict(42L);
    }

    @Test
    void shouldSkipDuplicateMessage() {
        doReturn(false).when(processed).claim(any());

        listener.onPriceChanged(message());

        verifyNoInteractions(cache);
    }

    private static PriceChangedMessage message() {
        return new PriceChangedMessage(UUID.randomUUID(), 7L, 42L, StoreCode.FLIP, new BigDecimal("2500"),
                new BigDecimal("2100"), true, Instant.parse("2026-10-23T06:00:00Z"));
    }
}
