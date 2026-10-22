package kz.nurbolat.bookaggregator.parser.core;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.offer.OfferIngestionService;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class StoreCrawlServiceIT extends AbstractIntegrationTest {

    private static final String KNOWN_ISBN = "9780306406157";
    private static final String NEW_ISBN = "9780804429573";

    @Autowired
    private StoreService storeService;

    @Autowired
    private ParserRunService parserRunService;

    @Autowired
    private OfferIngestionService ingestionService;

    @Autowired
    private CrawlTargetRepository targets;

    @Autowired
    private ParserProperties properties;

    @Autowired
    private JdbcTemplate jdbc;

    @PersistenceContext
    private EntityManager entityManager;

    private final FakeParser parser = new FakeParser();
    private StoreCrawlService crawlService;
    private long knownEdition;
    private long editionToFind;

    @BeforeEach
    void setUp() {
        crawlService = new StoreCrawlService(List.of(parser), storeService, parserRunService, ingestionService,
                targets, properties);
        knownEdition = edition("Мастер и Маргарита", "Михаил Булгаков", KNOWN_ISBN);
        editionToFind = edition("Собачье сердце", "Михаил Булгаков", NEW_ISBN);
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, price, match_status)
                SELECT ?, id, 'known-1', 'https://flip.test/known-1', 2000, 'MATCHED' FROM stores WHERE code = 'FLIP'
                """, knownEdition);
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, price, match_status)
                SELECT ?, id, 'known-2', 'https://flip.test/known-2', 2500, 'MATCHED' FROM stores WHERE code = 'FLIP'
                """, knownEdition);
    }

    @Test
    void shouldFinishPartiallyAndCountPagesWhenOnePageFails() {
        parser.products.put("https://flip.test/known-1", offer("known-1", KNOWN_ISBN, "1900"));
        parser.failing.add("https://flip.test/known-2");

        CrawlResult result = crawl(knownEdition);

        assertThat(result.status()).isEqualTo(RunStatus.PARTIAL);
        assertThat(result.pagesOk()).isEqualTo(1);
        assertThat(result.pagesFailed()).isEqualTo(1);
        assertThat(result.ingest().upserted()).isEqualTo(1);
        assertThat(result.ingest().priceChanged()).isEqualTo(1);
        Map<String, Object> run = jdbc.queryForMap("SELECT * FROM parser_runs WHERE id = ?", result.runId());
        assertThat(run).containsEntry("status", "PARTIAL").containsEntry("trigger_type", "MANUAL")
                .containsEntry("pages_ok", 1).containsEntry("pages_failed", 1)
                .containsEntry("offers_upserted", 1).containsEntry("prices_changed", 1);
        assertThat(run.get("finished_at")).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_events WHERE level = 'WARN' AND details ->> 'runId' = ?",
                Integer.class, result.runId().toString())).isEqualTo(1);
    }

    @Test
    void shouldDiscoverOfferForEditionMissingInStore() {
        parser.searchResults.put(NEW_ISBN, List.of(offer("new-1", null, "1500")));
        parser.products.put("https://flip.test/new-1", offer("new-1", NEW_ISBN, "1500"));

        CrawlResult result = crawl(editionToFind);

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(parser.searched).containsExactly(NEW_ISBN);
        assertThat(result.ingest().matched()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT edition_id FROM offers WHERE external_id = 'new-1'", Long.class))
                .isEqualTo(editionToFind);
    }

    @Test
    void shouldMarkOfferOutOfStockWhenProductPageIsGone() {
        parser.products.put("https://flip.test/known-1", offer("known-1", KNOWN_ISBN, "2000"));
        // known-2 has no page any more: fetchOffer returns empty

        CrawlResult result = crawl(knownEdition);

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(jdbc.queryForObject("SELECT in_stock FROM offers WHERE external_id = 'known-2'", Boolean.class))
                .isFalse();
    }

    @Test
    void shouldStopRunWhenStoreBlocksUs() {
        parser.blocked = true;

        CrawlResult result = crawl(knownEdition);

        assertThat(result.status()).isEqualTo(RunStatus.FAILED);
        assertThat(result.pagesFailed()).isEqualTo(1);
        assertThat(result.error()).contains("blocked");
        assertThat(parser.searched).isEmpty();
    }

    private CrawlResult crawl(long editionId) {
        CrawlResult result = crawlService.crawl(StoreCode.FLIP, TriggerType.MANUAL, Optional.of(List.of(editionId)));
        // make pending updates visible to the JDBC assertions of this transaction
        entityManager.flush();
        return result;
    }

    private long edition(String title, String author, String isbn) {
        Long bookId = jdbc.queryForObject(
                "INSERT INTO books (title, author, title_norm, author_norm) VALUES (?, ?, lower(?), lower(?)) RETURNING id",
                Long.class, title, author, title, author);
        return jdbc.queryForObject("INSERT INTO editions (book_id, isbn13) VALUES (?, ?) RETURNING id",
                Long.class, bookId, isbn);
    }

    private static ParsedOffer offer(String externalId, String isbn, String price) {
        return new ParsedOffer(externalId, "https://flip.test/" + externalId, "Книга", "Автор", isbn,
                new BigDecimal(price), null, true, null, null, null, null, Binding.UNKNOWN);
    }

    /**
     * Store parser with canned pages; no network.
     */
    static class FakeParser implements StoreParser {

        final Map<String, ParsedOffer> products = new HashMap<>();
        final Map<String, List<ParsedOffer>> searchResults = new HashMap<>();
        final List<String> failing = new ArrayList<>();
        final List<String> searched = new ArrayList<>();
        boolean blocked;

        @Override
        public StoreCode store() {
            return StoreCode.FLIP;
        }

        @Override
        public List<ParsedOffer> search(String query) {
            searched.add(query);
            return searchResults.getOrDefault(query, List.of());
        }

        @Override
        public Optional<ParsedOffer> fetchOffer(String url) {
            if (blocked) {
                throw new StoreBlockedException(StoreCode.FLIP, url, "HTTP 403");
            }
            if (failing.contains(url)) {
                throw new FetchException("HTTP 400 from " + url);
            }
            return Optional.ofNullable(products.get(url));
        }
    }
}
