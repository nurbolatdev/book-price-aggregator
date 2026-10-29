package kz.nurbolat.bookaggregator.search;

import com.jayway.jsonpath.JsonPath;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class SearchCatalogModeIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldReturnCatalogSortedByStoresCountAndSkipSearchLogWhenQueryIsMissing() throws Exception {
        long logBefore = searchLogRows();

        String body = mockMvc.perform(get("/api/v1/search").param("inStock", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/v1/search").param("q", "  ").param("inStock", "false"))
                .andExpect(status().isOk());
        Thread.sleep(300);

        List<Integer> stores = JsonPath.read(body, "$.content[*].storesCount");
        assertThat(stores).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(searchLogRows()).isEqualTo(logBefore);
    }

    @Test
    void shouldReturnUnionWhenFiltersAreLists() throws Exception {
        int ru = total(get("/api/v1/search").param("language", "ru"));
        int kk = total(get("/api/v1/search").param("language", "kk"));
        int both = total(get("/api/v1/search").param("language", "ru,kk"));
        int hard = total(get("/api/v1/search").param("binding", "HARDCOVER"));
        int paper = total(get("/api/v1/search").param("binding", "PAPERBACK"));
        int bindings = total(get("/api/v1/search").param("binding", "HARDCOVER,PAPERBACK"));

        assertThat(ru).isPositive();
        assertThat(both).isEqualTo(ru + kk);
        assertThat(bindings).isEqualTo(hard + paper);
    }

    @Test
    void shouldRejectRelevanceSortWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("sort", "RELEVANCE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"));
        mockMvc.perform(get("/api/v1/search").param("sort", "PRICE_ASC"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTooShortQueryAndUnknownFilterValues() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "a"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/search").param("language", "xx"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/search").param("binding", "FOO"))
                .andExpect(status().isUnprocessableEntity());
    }

    private int total(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request.param("inStock", "false").param("size", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.totalElements")).intValue();
    }

    private long searchLogRows() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM search_log", Long.class);
        return count == null ? 0 : count;
    }
}
