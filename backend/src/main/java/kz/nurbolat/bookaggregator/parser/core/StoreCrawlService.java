package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.offer.IngestResult;
import kz.nurbolat.bookaggregator.offer.OfferIngestionService;
import kz.nurbolat.bookaggregator.parser.core.CrawlTargetRepository.EditionToFind;
import kz.nurbolat.bookaggregator.parser.core.CrawlTargetRepository.KnownOffer;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * One parser run over a store in two passes:
 * <ol>
 *   <li>refresh — reload product pages of known offers (beacon editions first, then the oldest);</li>
 *   <li>discovery — search the store for editions it has no offer for, then load the product
 *       pages of fitting results.</li>
 * </ol>
 * A failing page only counts in {@code pages_failed}; a blocked or paused store stops the run.
 * Offers are stored in small batches so that transactions stay short.
 */
@Slf4j
@Service
public class StoreCrawlService {

    static final int BATCH_SIZE = 20;
    // Product pages loaded per search: an ISBN search is exact, a text search is not
    private static final int CANDIDATES_BY_ISBN = 2;
    private static final int CANDIDATES_BY_TITLE = 3;

    private final Map<StoreCode, StoreParser> parsers = new EnumMap<>(StoreCode.class);
    private final StoreService storeService;
    private final ParserRunService parserRunService;
    private final OfferIngestionService offerIngestionService;
    private final CrawlTargetRepository targets;
    private final ParserProperties properties;

    public StoreCrawlService(List<StoreParser> parsers, StoreService storeService, ParserRunService parserRunService,
                             OfferIngestionService offerIngestionService, CrawlTargetRepository targets,
                             ParserProperties properties) {
        parsers.forEach(parser -> this.parsers.put(parser.store(), parser));
        this.storeService = storeService;
        this.parserRunService = parserRunService;
        this.offerIngestionService = offerIngestionService;
        this.targets = targets;
        this.properties = properties;
    }

    /**
     * @param editionIds restrict the run to these editions; empty to use the run limits over the whole catalog
     */
    public CrawlResult crawl(StoreCode storeCode, TriggerType trigger, Optional<List<Long>> editionIds) {
        StoreParser parser = parsers.get(storeCode);
        if (parser == null) {
            throw new NotFoundException("No parser for store " + storeCode);
        }
        Store store = storeService.getByCode(storeCode);
        if (!store.isEnabled()) {
            throw new AppException(ErrorCode.CONFLICT, "Store %s is disabled".formatted(storeCode));
        }

        ParserRun run = parserRunService.start(storeCode, trigger);
        Run state = new Run(storeCode, run.getId());
        List<Long> ids = editionIds.orElse(null);
        try {
            refresh(parser, store, ids, state);
            discover(parser, store, ids, state);
        } catch (StoreBlockedException | StoreUnavailableException e) {
            log.warn("Parser run {} stopped: {}", run.getId(), e.getMessage());
            state.abort(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Parser run {} failed", run.getId(), e);
            state.abort(e.toString());
        } finally {
            state.flush();
        }

        ParserRun finished = parserRunService.finish(run.getId(), state.stats());
        return new CrawlResult(run.getId(), storeCode, finished.getStatus(), state.pagesOk, state.pagesFailed,
                state.ingested, finished.getError());
    }

    private void refresh(StoreParser parser, Store store, List<Long> editionIds, Run state) {
        List<KnownOffer> offers = targets.findOffersToRefresh(store.getId(), editionIds, properties.maxEditionsPerRun());
        log.info("Run {}: refreshing {} known offers of {}", state.runId, offers.size(), store.getCode());
        List<Long> gone = new ArrayList<>();
        for (KnownOffer offer : offers) {
            Optional<ParsedOffer> page = state.page(() -> parser.fetchOffer(offer.url()));
            if (page == null) {
                continue;
            }
            page.ifPresentOrElse(state::add, () -> gone.add(offer.offerId()));
        }
        if (!gone.isEmpty()) {
            offerIngestionService.markOutOfStock(gone);
        }
    }

    private void discover(StoreParser parser, Store store, List<Long> editionIds, Run state) {
        List<EditionToFind> editions = targets.findEditionsToDiscover(
                store.getId(), editionIds, properties.maxDiscoveryPerRun());
        log.info("Run {}: looking for {} editions in {}", state.runId, editions.size(), store.getCode());
        for (EditionToFind edition : editions) {
            boolean byIsbn = edition.isbn13() != null;
            String query = byIsbn ? edition.isbn13()
                    : String.join(" ", Stream.of(edition.title(), edition.author()).filter(Objects::nonNull).toList());
            Optional<List<ParsedOffer>> hits = Optional.ofNullable(state.page(() -> parser.search(query)));
            List<ParsedOffer> candidates = hits.orElse(List.of()).stream()
                    .filter(hit -> byIsbn || sameTitle(hit, edition))
                    .limit(byIsbn ? CANDIDATES_BY_ISBN : CANDIDATES_BY_TITLE)
                    .toList();

            // Products already stored are refreshed by the first pass
            Set<String> known = targets.findKnownExternalIds(store.getId(),
                    candidates.stream().map(ParsedOffer::externalId).toList());
            for (ParsedOffer candidate : candidates) {
                if (known.contains(candidate.externalId()) || state.contains(candidate.externalId())) {
                    continue;
                }
                Optional<ParsedOffer> detail = state.page(() -> parser.fetchOffer(candidate.url()));
                if (detail != null) {
                    detail.ifPresent(state::add);
                }
            }
        }
    }

    private static boolean sameTitle(ParsedOffer hit, EditionToFind edition) {
        return Objects.equals(TextNormalizer.normalize(hit.title()), edition.titleNorm());
    }

    /**
     * Counters and the pending batch of one run.
     */
    private final class Run {

        private final StoreCode store;
        private final Long runId;
        private final List<ParsedOffer> batch = new ArrayList<>();
        private final Set<String> seen = new HashSet<>();
        private int pagesOk;
        private int pagesFailed;
        private IngestResult ingested = IngestResult.EMPTY;
        private String lastError;
        private boolean aborted;

        private Run(StoreCode store, Long runId) {
            this.store = store;
            this.runId = runId;
        }

        /**
         * Loads one page; returns {@code null} if it failed (counted), rethrows store-wide stops.
         */
        private <T> T page(Supplier<T> load) {
            try {
                T result = load.get();
                pagesOk++;
                return result;
            } catch (StoreBlockedException | StoreUnavailableException e) {
                pagesFailed++;
                throw e;
            } catch (RuntimeException e) {
                pagesFailed++;
                lastError = e.getMessage();
                log.warn("Run {}: page failed: {}", runId, e.getMessage());
                return null;
            }
        }

        private void add(ParsedOffer offer) {
            seen.add(offer.externalId());
            batch.add(offer);
            if (batch.size() >= BATCH_SIZE) {
                flush();
            }
        }

        private boolean contains(String externalId) {
            return seen.contains(externalId);
        }

        private void flush() {
            if (batch.isEmpty()) {
                return;
            }
            ingested = ingested.plus(offerIngestionService.ingest(store, List.copyOf(batch), runId));
            batch.clear();
        }

        private void abort(String reason) {
            aborted = true;
            lastError = reason;
        }

        private RunStats stats() {
            return new RunStats(pagesOk, pagesFailed, ingested.upserted(), ingested.priceChanged(), aborted, lastError);
        }
    }
}
