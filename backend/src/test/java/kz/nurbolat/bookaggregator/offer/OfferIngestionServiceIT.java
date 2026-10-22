package kz.nurbolat.bookaggregator.offer;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@RecordApplicationEvents
class OfferIngestionServiceIT extends AbstractIntegrationTest {

    private static final String ISBN = "9780306406157";

    @Autowired
    private OfferIngestionService ingestionService;

    @Autowired
    private EditionRepository editionRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationEvents events;

    @PersistenceContext
    private EntityManager entityManager;

    private long editionId;

    @BeforeEach
    void setUp() {
        Long bookId = jdbc.queryForObject("""
                INSERT INTO books (title, author, title_norm, author_norm)
                VALUES ('Мастер и Маргарита', 'Михаил Булгаков', 'мастер и маргарита', 'михаил булгаков')
                RETURNING id""", Long.class);
        editionId = jdbc.queryForObject(
                "INSERT INTO editions (book_id, isbn13, publisher) VALUES (?, ?, 'АСТ') RETURNING id",
                Long.class, bookId, ISBN);
    }

    @Test
    void shouldStoreNewOfferWithHistoryAndEvent() {
        IngestResult result = ingest(offer("100", ISBN, "2021"));

        assertThat(result).isEqualTo(new IngestResult(1, 1, 1, 0));
        assertThat(historyPrices("100")).containsExactly(new BigDecimal("2021.00"));
        assertThat(events.stream(PriceChangedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.editionId()).isEqualTo(editionId);
            assertThat(event.store()).isEqualTo(StoreCode.FLIP);
            assertThat(event.oldPrice()).isNull();
            assertThat(event.newPrice()).isEqualByComparingTo("2021");
        });
    }

    @Test
    void shouldNotWriteHistoryOrEventWhenPriceIsUnchanged() {
        ingest(offer("100", ISBN, "2021"));
        events.clear();

        IngestResult second = ingest(offer("100", ISBN, "2021"));

        assertThat(second).isEqualTo(new IngestResult(1, 0, 0, 0));
        assertThat(historyPrices("100")).hasSize(1);
        assertThat(events.stream(PriceChangedEvent.class)).isEmpty();
    }

    @Test
    void shouldWriteHistoryAndEventWhenPriceChanges() {
        ingest(offer("100", ISBN, "2021"));
        events.clear();

        IngestResult second = ingest(offer("100", ISBN, "1890"));

        assertThat(second.priceChanged()).isEqualTo(1);
        assertThat(historyPrices("100")).containsExactly(new BigDecimal("2021.00"), new BigDecimal("1890.00"));
        assertThat(events.stream(PriceChangedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.oldPrice()).isEqualByComparingTo("2021");
            assertThat(event.newPrice()).isEqualByComparingTo("1890");
        });
    }

    @Test
    void shouldUpdateExistingOfferInsteadOfCreatingDuplicate() {
        ingest(offer("100", ISBN, "2021"));
        ParsedOffer outOfStock = new ParsedOffer("100", "https://www.flip.kz/catalog?prod=100", "Мастер и Маргарита",
                "Михаил Булгаков", ISBN, new BigDecimal("2021"), null, false, "Под заказ", null, "АСТ", 2018,
                Binding.HARDCOVER);

        ingest(outOfStock, outOfStock);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM offers WHERE external_id = '100'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT in_stock FROM offers WHERE external_id = '100'", Boolean.class)).isFalse();
    }

    @Test
    void shouldLeaveOfferForReviewWhenItCannotBeMatched() {
        ParsedOffer unknown = new ParsedOffer("200", "https://www.flip.kz/catalog?prod=200", "Неизвестная книга",
                null, null, new BigDecimal("1500"), null, true, null, null, null, null, Binding.UNKNOWN);

        IngestResult result = ingest(unknown);

        assertThat(result).isEqualTo(new IngestResult(1, 1, 0, 1));
        assertThat(jdbc.queryForObject("SELECT match_status FROM offers WHERE external_id = '200'", String.class))
                .isEqualTo("PENDING_REVIEW");
        assertThat(historyPrices("200")).hasSize(1);
        assertThat(events.stream(PriceChangedEvent.class)).isEmpty();
    }

    @Test
    void shouldCreateEditionFromStorePageWhenIsbnIsNew() {
        // Google Books is unreachable in tests, so the store page supplies the metadata
        ParsedOffer offer = new ParsedOffer("300", "https://www.flip.kz/catalog?prod=300", "Мастер и Маргарита",
                "Михаил Булгаков", "9780804429573", new BigDecimal("3500"), null, true, null,
                "https://s.f.kz/prod/300.jpg", "Азбука", 2022, Binding.PAPERBACK);

        IngestResult result = ingest(offer);

        assertThat(result.matched()).isEqualTo(1);
        var edition = editionRepository.findByIsbn13("9780804429573").orElseThrow();
        assertThat(edition.getBinding()).isEqualTo(Binding.PAPERBACK);
        assertThat(edition.getPublisher()).isEqualTo("Азбука");
        // the same work gets a second edition instead of a duplicate book
        assertThat(edition.getBook().getId()).isEqualTo(
                editionRepository.findById(editionId).orElseThrow().getBook().getId());
    }

    @Test
    void shouldMarkOffersNotSeenForAWeekAsOutOfStock() {
        ingest(offer("100", ISBN, "2021"));
        jdbc.update("UPDATE offers SET last_seen_at = NOW() - INTERVAL '8 days' WHERE external_id = '100'");

        int marked = ingestionService.markUnseenOutOfStock(StoreCode.FLIP, Duration.ofDays(7));

        assertThat(marked).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT in_stock FROM offers WHERE external_id = '100'", Boolean.class))
                .isFalse();
    }

    private IngestResult ingest(ParsedOffer... offers) {
        IngestResult result = ingestionService.ingest(StoreCode.FLIP, List.of(offers), 1L);
        // make pending updates visible to the JDBC assertions of this transaction
        entityManager.flush();
        return result;
    }

    private List<BigDecimal> historyPrices(String externalId) {
        return jdbc.queryForList("""
                SELECT ph.price FROM price_history ph JOIN offers o ON o.id = ph.offer_id
                WHERE o.external_id = ? ORDER BY ph.id""", BigDecimal.class, externalId);
    }

    private static ParsedOffer offer(String externalId, String isbn, String price) {
        return new ParsedOffer(externalId, "https://www.flip.kz/catalog?prod=" + externalId, "Мастер и Маргарита",
                "Михаил Булгаков", isbn, new BigDecimal(price), null, true, "Доставим завтра", null, "АСТ", 2018,
                Binding.HARDCOVER);
    }
}
