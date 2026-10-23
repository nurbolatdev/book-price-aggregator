package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.messaging.ParseJobPublisher;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.parser.core.TriggerType;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ManualRunServiceTest {

    private final StoreCrawlService crawlService = mock(StoreCrawlService.class);
    private final StoreService storeService = mock(StoreService.class);
    private final ParseJobPublisher publisher = mock(ParseJobPublisher.class);
    private final ManualRunService service = new ManualRunService(crawlService, storeService, publisher);
    private final Store flip = new Store();

    @BeforeEach
    void setUp() {
        flip.setCode(StoreCode.FLIP);
        when(storeService.getByCode(StoreCode.FLIP)).thenReturn(flip);
        when(crawlService.hasParser(StoreCode.FLIP)).thenReturn(true);
        when(publisher.publish(any(), any(), any())).thenReturn(true);
    }

    @Test
    void shouldQueueManualCrawl() {
        service.queue(StoreCode.FLIP, List.of(5L));

        verify(publisher).publish(StoreCode.FLIP, TriggerType.MANUAL, List.of(5L));
    }

    @Test
    void shouldRefuseStoreWithoutParser() {
        assertThatThrownBy(() -> service.queue(StoreCode.KASPI, List.of())).isInstanceOf(NotFoundException.class);
        verify(publisher, never()).publish(any(), any(), any());
    }

    @Test
    void shouldRefuseSwitchedOffStore() {
        flip.setEnabled(false);

        assertThatThrownBy(() -> service.queue(StoreCode.FLIP, List.of()))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode())
                        .isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void shouldAnswerUnavailableWhenQueueCannotBeReached() {
        when(publisher.publish(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.queue(StoreCode.FLIP, List.of()))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getCode())
                        .isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    }
}
