package kz.nurbolat.bookaggregator.messaging;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.messaging.message.PriceChangedMessage;
import kz.nurbolat.bookaggregator.offer.OfferIngestionService;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional on purpose: the message must leave only after a real commit.
 */
class PriceChangedFlowIT extends AbstractIntegrationTest {

    private static final String ISBN = "9785000000019";

    @Autowired
    private OfferIngestionService ingestionService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private JdbcTemplate jdbc;

    private long editionId;

    @BeforeEach
    void setUp() {
        rabbitAdmin.initialize();
        rabbitAdmin.purgeQueue(RabbitTopology.PRICE_CHANGED_CACHE_QUEUE);
        Long bookId = jdbc.queryForObject("""
                INSERT INTO books (title, author, title_norm, author_norm)
                VALUES ('Трудно быть богом', 'Аркадий Стругацкий', 'трудно быть богом', 'аркадий стругацкий')
                RETURNING id""", Long.class);
        editionId = jdbc.queryForObject("INSERT INTO editions (book_id, isbn13) VALUES (?, ?) RETURNING id",
                Long.class, bookId, ISBN);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM price_history WHERE offer_id IN (SELECT id FROM offers WHERE edition_id = ?)", editionId);
        jdbc.update("DELETE FROM offers WHERE edition_id = ?", editionId);
        Long bookId = jdbc.queryForObject("SELECT book_id FROM editions WHERE id = ?", Long.class, editionId);
        jdbc.update("DELETE FROM editions WHERE id = ?", editionId);
        jdbc.update("DELETE FROM books WHERE id = ?", bookId);
    }

    @Test
    void shouldPublishPriceChangeAfterCommit() {
        ingestionService.ingest(StoreCode.FLIP, List.of(offer("2500")), null);

        Object message = rabbitTemplate.receiveAndConvert(RabbitTopology.PRICE_CHANGED_CACHE_QUEUE, 5000);
        assertThat(message).isInstanceOfSatisfying(PriceChangedMessage.class, change -> {
            assertThat(change.editionId()).isEqualTo(editionId);
            assertThat(change.store()).isEqualTo(StoreCode.FLIP);
            assertThat(change.oldPrice()).isNull();
            assertThat(change.newPrice()).isEqualByComparingTo("2500");
            assertThat(change.inStock()).isTrue();
        });
    }

    @Test
    void shouldPublishNothingWhenTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            ingestionService.ingest(StoreCode.FLIP, List.of(offer("2500")), null);
            status.setRollbackOnly();
        });

        assertThat(rabbitTemplate.receiveAndConvert(RabbitTopology.PRICE_CHANGED_CACHE_QUEUE, 2000)).isNull();
    }

    private static ParsedOffer offer(String price) {
        return new ParsedOffer("flow-1", "https://www.flip.kz/catalog?prod=flow-1", "Трудно быть богом",
                "Аркадий Стругацкий", ISBN, new BigDecimal(price), null, true, null, null, null, null,
                Binding.UNKNOWN);
    }
}
