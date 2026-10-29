package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.PriceFixtures.addOffer;
import static kz.nurbolat.bookaggregator.support.PriceFixtures.daysAgo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class SimilarEditionsIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private long book(String title, String authorNorm) {
        return jdbc.queryForObject("""
                INSERT INTO books (title, title_norm, author, author_norm) VALUES (?, lower(?), ?, ?) RETURNING id""",
                Long.class, title, title, authorNorm, authorNorm);
    }

    private long edition(long bookId, String language) {
        return jdbc.queryForObject("""
                INSERT INTO editions (book_id, publisher, year, binding, language)
                VALUES (?, 'Тест', 2024, 'PAPERBACK', ?) RETURNING id""", Long.class, bookId, language);
    }

    private void offer(long editionId, String store, boolean inStock, int price) {
        addOffer(jdbc, editionId, store, inStock, daysAgo(1, price));
    }

    @Test
    void shouldPutSameAuthorFirstThenSameLanguageByOffersCount() throws Exception {
        long current = edition(book("Текущая", "иванов"), "ru");
        offer(current, "FLIP", true, 1000);

        long sameAuthor = edition(book("Тот же автор", "иванов"), "kk");
        offer(sameAuthor, "FLIP", true, 3000);
        long manyOffers = edition(book("Много предложений", "петров"), "ru");
        offer(manyOffers, "FLIP", true, 500);
        offer(manyOffers, "OZON", true, 600);
        offer(manyOffers, "MELOMAN", true, 700);
        long fewOffers = edition(book("Мало предложений", "сидоров"), "ru");
        offer(fewOffers, "FLIP", true, 400);
        long otherLanguage = edition(book("Другой язык", "кузнецов"), "en");
        offer(otherLanguage, "FLIP", true, 100);

        mvc.perform(get("/api/v1/editions/" + current + "/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].editionId").value(sameAuthor))
                .andExpect(jsonPath("$[1].editionId").value(manyOffers))
                .andExpect(jsonPath("$[2].editionId").value(fewOffers))
                .andExpect(jsonPath("$[0].title").value("Тот же автор"));
    }

    @Test
    void shouldExcludeCurrentBookAndBooksWithoutInStockPrice() throws Exception {
        long bookId = book("Текущая", "иванов");
        long current = edition(bookId, "ru");
        offer(current, "FLIP", true, 1000);
        long sibling = edition(bookId, "ru");
        offer(sibling, "FLIP", true, 900);
        long outOfStock = edition(book("Нет в наличии", "иванов"), "ru");
        offer(outOfStock, "FLIP", false, 800);

        mvc.perform(get("/api/v1/editions/" + current + "/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldShowCheapestEditionOfBookAndRespectLimit() throws Exception {
        long current = edition(book("Текущая", "иванов"), "ru");
        offer(current, "FLIP", true, 1000);
        long otherBook = book("Другая", "иванов");
        long expensive = edition(otherBook, "ru");
        offer(expensive, "FLIP", true, 2000);
        long cheap = edition(otherBook, "ru");
        offer(cheap, "OZON", true, 1500);
        long third = edition(book("Третья", "иванов"), "ru");
        offer(third, "FLIP", true, 100);

        mvc.perform(get("/api/v1/editions/" + current + "/similar?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/v1/editions/" + current + "/similar"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.bookId == " + otherBook + ")].editionId").value(org.hamcrest.Matchers.contains((int) cheap)))
                .andExpect(jsonPath("$[?(@.bookId == " + otherBook + ")].bestPrice").value(org.hamcrest.Matchers.contains(1500.0)));
    }

    @Test
    void shouldAnswerNotFoundForUnknownEditionAndRejectBadLimit() throws Exception {
        mvc.perform(get("/api/v1/editions/999999999/similar")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/editions/1/similar?limit=13")).andExpect(status().isUnprocessableEntity());
    }
}
