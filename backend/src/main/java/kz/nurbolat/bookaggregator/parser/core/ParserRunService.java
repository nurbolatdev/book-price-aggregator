package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.admin.SystemEventService;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Records parser runs: RUNNING at the start, SUCCESS / PARTIAL / FAILED with statistics at the end.
 * Partial and failed runs are also reported to the admin event journal.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParserRunService {

    static final String EVENT_SOURCE = "parser";

    private final ParserRunRepository repository;
    private final StoreService storeService;
    private final SystemEventService systemEventService;
    private final Clock clock;

    @Transactional
    public ParserRun start(StoreCode store, TriggerType trigger) {
        ParserRun run = new ParserRun();
        run.setStore(storeService.getByCode(store));
        run.setTriggerType(trigger);
        run.setStartedAt(clock.instant());
        ParserRun saved = repository.save(run);
        log.info("Parser run {} started: store={} trigger={}", saved.getId(), store, trigger);
        return saved;
    }

    @Transactional
    public ParserRun finish(Long runId, RunStats stats) {
        ParserRun run = repository.findById(runId)
                .orElseThrow(() -> new NotFoundException("Parser run not found: " + runId));
        run.setPagesOk(stats.pagesOk());
        run.setPagesFailed(stats.pagesFailed());
        run.setOffersUpserted(stats.offersUpserted());
        run.setPricesChanged(stats.pricesChanged());
        run.setError(stats.error());
        run.setStatus(statusOf(stats));
        run.setFinishedAt(clock.instant());

        StoreCode store = run.getStore().getCode();
        log.info("Parser run {} finished: store={} status={} pagesOk={} pagesFailed={} upserted={} priceChanged={}",
                runId, store, run.getStatus(), stats.pagesOk(), stats.pagesFailed(), stats.offersUpserted(),
                stats.pricesChanged());
        report(run, store);
        return run;
    }

    static RunStatus statusOf(RunStats stats) {
        if (stats.aborted() || (stats.pagesOk() == 0 && stats.pagesFailed() > 0)) {
            return RunStatus.FAILED;
        }
        return stats.pagesFailed() > 0 ? RunStatus.PARTIAL : RunStatus.SUCCESS;
    }

    private void report(ParserRun run, StoreCode store) {
        if (run.getStatus() == RunStatus.SUCCESS) {
            return;
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("runId", run.getId());
        details.put("store", store.name());
        details.put("pagesOk", run.getPagesOk());
        details.put("pagesFailed", run.getPagesFailed());
        if (run.getError() != null) {
            details.put("error", run.getError());
        }
        String message = "Parser run for %s finished with status %s".formatted(store, run.getStatus());
        // A blocked store is an expected outcome of the parsing policy, not an application error
        systemEventService.warn(EVENT_SOURCE, message, details);
    }
}
