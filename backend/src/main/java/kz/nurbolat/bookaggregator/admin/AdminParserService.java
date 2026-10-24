package kz.nurbolat.bookaggregator.admin;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import kz.nurbolat.bookaggregator.admin.dto.AdminParserResponse;
import kz.nurbolat.bookaggregator.parser.core.RunStatus;
import kz.nurbolat.bookaggregator.parser.core.StoreCrawlService;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreRepository;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminParserService {

    private static final Duration SUCCESS_WINDOW = Duration.ofHours(24);
    private static final Duration AVG_WINDOW = Duration.ofDays(7);

    private final JdbcTemplate jdbc;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final StoreCrawlService crawlService;
    private final CircuitBreakerRegistry circuitBreakers;
    private final SystemEventService events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AdminParserResponse> parsers() {
        Instant now = clock.instant();
        Map<StoreCode, Stats> stats = stats(now);
        return storeService.findAll().stream().map(store -> {
            Stats s = stats.get(store.getCode());
            return new AdminParserResponse(store.getCode(), store.getName(), store.isEnabled(),
                    crawlService.hasParser(store.getCode()), s == null ? null : s.lastRun(), s == null ? null : s.successPercent(),
                    s == null ? 0 : s.matched(), s == null ? null : s.avgRunSeconds(), breakerState(store.getCode()));
        }).toList();
    }

    /** Switches the store on or off, notes it in the journal and drops the reference cache. */
    @Transactional
    public AdminParserResponse.StoreState setEnabled(StoreCode code, boolean enabled) {
        Store store = storeRepository.findByCode(code)
                .orElseThrow(() -> new kz.nurbolat.bookaggregator.common.error.NotFoundException("Store not found: " + code));
        if (store.isEnabled() != enabled) {
            store.setEnabled(enabled);
            storeRepository.save(store);
            events.info("store", "Магазин " + code + (enabled ? " включён" : " выключен"),
                    Map.of("store", code.name(), "enabled", enabled));
        }
        evictCacheAfterCommit();
        return new AdminParserResponse.StoreState(code, store.isEnabled());
    }

    /** The cache is dropped once the change is visible to other transactions; without a transaction at once. */
    private void evictCacheAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    storeService.evictCache();
                }
            });
        } else {
            storeService.evictCache();
        }
    }

    private String breakerState(StoreCode code) {
        return circuitBreakers.find("store-" + code).map(cb -> cb.getState().name()).orElse("CLOSED");
    }

    private Map<StoreCode, Stats> stats(Instant now) {
        Timestamp successSince = Timestamp.from(now.minus(SUCCESS_WINDOW));
        Timestamp avgSince = Timestamp.from(now.minus(AVG_WINDOW));
        List<Stats> rows = jdbc.query("""
                SELECT s.code,
                       last.status, last.started_at, last.finished_at,
                       (SELECT SUM(r.pages_ok) FROM parser_runs r WHERE r.store_id = s.id AND r.started_at >= ?),
                       (SELECT SUM(r.pages_failed) FROM parser_runs r WHERE r.store_id = s.id AND r.started_at >= ?),
                       (SELECT COUNT(*) FROM offers o WHERE o.store_id = s.id AND o.match_status = 'MATCHED'),
                       (SELECT AVG(EXTRACT(EPOCH FROM (r.finished_at - r.started_at))) FROM parser_runs r
                         WHERE r.store_id = s.id AND r.finished_at IS NOT NULL AND r.started_at >= ?)
                FROM stores s
                LEFT JOIN LATERAL (SELECT status, started_at, finished_at FROM parser_runs r
                                   WHERE r.store_id = s.id ORDER BY r.started_at DESC LIMIT 1) last ON TRUE
                """,
                (rs, row) -> {
                    Timestamp started = rs.getTimestamp(3);
                    Timestamp finished = rs.getTimestamp(4);
                    AdminParserResponse.LastRun lastRun = started == null ? null : new AdminParserResponse.LastRun(
                            RunStatus.valueOf(rs.getString(2)), started.toInstant(),
                            finished == null ? null : Duration.between(started.toInstant(), finished.toInstant()).toSeconds());
                    return new Stats(StoreCode.valueOf(rs.getString(1)), lastRun,
                            successPercent(rs.getLong(5), rs.getLong(6)), rs.getLong(7), average(rs.getBigDecimal(8)));
                }, successSince, successSince, avgSince);
        return rows.stream().collect(Collectors.toMap(Stats::code, Function.identity()));
    }

    /** pages_ok / (pages_ok + pages_failed), one decimal place. */
    static Double successPercent(long ok, long failed) {
        long total = ok + failed;
        return total == 0 ? null : Math.round(ok * 1000.0 / total) / 10.0;
    }

    private static Double average(java.math.BigDecimal value) {
        return value == null ? null : Math.round(value.doubleValue() * 10) / 10.0;
    }

    private record Stats(StoreCode code, AdminParserResponse.LastRun lastRun, Double successPercent, long matched,
                         Double avgRunSeconds) {
    }
}
