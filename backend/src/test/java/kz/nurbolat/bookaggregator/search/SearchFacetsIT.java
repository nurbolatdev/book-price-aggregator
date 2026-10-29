package kz.nurbolat.bookaggregator.search;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class SearchFacetsIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void shouldCountSelectedStoreAsSearchTotalAndKeepOtherStoresIgnoringOwnFilter() throws Exception {
        String facets = facets(get("/api/v1/search/facets").param("stores", "FLIP"));

        for (String store : List.of("FLIP", "OZON", "MELOMAN")) {
            assertThat(storeCount(facets, store))
                    .as(store)
                    .isEqualTo(searchTotal(get("/api/v1/search").param("stores", store)));
        }
        assertThat(storeCount(facets, "OZON")).isPositive();
    }

    @Test
    void shouldCountBindingsAndLanguagesAsSearchTotalsWithThatSingleValue() throws Exception {
        String facets = facets(get("/api/v1/search/facets").param("q", "мастер маргарита"));

        for (String binding : List.of("HARDCOVER", "PAPERBACK")) {
            assertThat(valueCount(facets, "bindings", binding)).isEqualTo(searchTotal(
                    get("/api/v1/search").param("q", "мастер маргарита").param("binding", binding)));
        }
        for (String language : List.of("ru", "kk", "en")) {
            assertThat(valueCount(facets, "languages", language)).isEqualTo(searchTotal(
                    get("/api/v1/search").param("q", "мастер маргарита").param("language", language)));
        }
        assertThat(valueCount(facets, "languages", "ru")).isPositive();
    }

    @Test
    void shouldListOnlyEnabledStoresAndZeroValuesInCatalogMode() throws Exception {
        String body = facets(get("/api/v1/search/facets"));

        List<String> stores = JsonPath.read(body, "$.stores[*].code");
        List<String> enabled = jdbc.queryForList("SELECT code FROM stores WHERE enabled ORDER BY id", String.class);
        assertThat(stores).containsExactlyElementsOf(enabled).doesNotContain("KASPI", "WILDBERRIES");
        List<String> bindings = JsonPath.read(body, "$.bindings[*].value");
        List<String> languages = JsonPath.read(body, "$.languages[*].value");
        assertThat(bindings).containsExactly("HARDCOVER", "PAPERBACK");
        assertThat(languages).containsExactly("ru", "kk", "en");
    }

    @Test
    void shouldCountEditionsNotOffers() throws Exception {
        String before = facets(get("/api/v1/search/facets"));
        jdbc.update("""
                INSERT INTO offers (edition_id, store_id, external_id, url, title_raw, price, in_stock,
                                    match_status, first_seen_at, last_seen_at, last_changed_at)
                SELECT o.edition_id, o.store_id, 'extra-' || o.id, o.url, o.title_raw, o.price + 1, o.in_stock,
                       'MATCHED', NOW(), NOW(), NOW()
                FROM offers o JOIN stores s ON s.id = o.store_id
                WHERE s.code = 'FLIP'
                """);
        redis.delete(redis.keys("search-facets:v1:*"));

        String after = facets(get("/api/v1/search/facets"));

        assertThat(storeCount(after, "FLIP")).isEqualTo(storeCount(before, "FLIP"));
    }

    @Test
    void shouldReturnZerosForPunctuationOnlyQuery() throws Exception {
        String facets = facets(get("/api/v1/search/facets").param("q", "!!"));

        assertThat(storeCount(facets, "FLIP")).isZero();
        assertThat(valueCount(facets, "languages", "ru")).isZero();
        assertThat(valueCount(facets, "bindings", "HARDCOVER")).isZero();
    }

    @Test
    void shouldServeRepeatedRequestFromRedis() throws Exception {
        String first = facets(get("/api/v1/search/facets").param("q", "мастер"));
        assertThat(redis.keys("search-facets:v1:*")).hasSize(1);

        jdbc.update("DELETE FROM offers");
        String second = facets(get("/api/v1/search/facets").param("q", "мастер"));

        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldRejectInvalidFilters() throws Exception {
        mockMvc.perform(get("/api/v1/search/facets").param("q", "a"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/search/facets").param("language", "xx"))
                .andExpect(status().isUnprocessableEntity());
    }

    private String facets(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long searchTotal(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request.param("size", "1")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.totalElements")).longValue();
    }

    private static long storeCount(String body, String code) {
        List<Number> counts = JsonPath.read(body, "$.stores[?(@.code == '" + code + "')].count");
        return counts.get(0).longValue();
    }

    private static long valueCount(String body, String list, String value) {
        List<Number> counts = JsonPath.read(body, "$." + list + "[?(@.value == '" + value + "')].count");
        return counts.get(0).longValue();
    }
}
