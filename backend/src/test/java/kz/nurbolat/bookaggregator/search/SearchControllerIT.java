package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class SearchControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditionRepository editionRepository;

    @Test
    void shouldReturnPageInContractFormatWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "мастер маргарита").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements", notNullValue()))
                .andExpect(jsonPath("$.totalPages", notNullValue()))
                .andExpect(jsonPath("$.content[0].title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.content[0].editionId", notNullValue()))
                .andExpect(jsonPath("$.content[0].bookId", notNullValue()))
                .andExpect(jsonPath("$.content[0].bestPrice.store", notNullValue()))
                .andExpect(jsonPath("$.content[0].bestPrice.inStock").value(true))
                .andExpect(jsonPath("$.content[0].offers.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.content[0].offers[0].store", notNullValue()))
                .andExpect(jsonPath("$.content[0].storesCount", notNullValue()))
                .andExpect(jsonPath("$.content[0].updatedAt", notNullValue()));
    }

    @Test
    void shouldAcceptFiltersFromQueryString() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "толстой")
                        .param("stores", "FLIP,KASPI")
                        .param("binding", "HARDCOVER")
                        .param("sort", "PRICE_ASC")
                        .param("inStock", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn422WhenQueryIsTooShort() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "м"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("q"));
    }

    @Test
    void shouldReturn422WhenFiltersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "мастер").param("size", "51"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
        mockMvc.perform(get("/api/v1/search").param("q", "мастер").param("stores", "AMAZON"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("stores"));
    }

    @Test
    void shouldSuggestTitlesAndRejectShortInput() throws Exception {
        mockMvc.perform(get("/api/v1/search/suggest").param("q", "мас"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].label").value("Мастер и Маргарита — Михаил Булгаков"))
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(8)));
        mockMvc.perform(get("/api/v1/search/suggest").param("q", "м"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("q"));
    }

    @Test
    void shouldServeEditionCardAndPriceHistory() throws Exception {
        Long id = editionRepository.findByIsbn13("9785601000017").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/editions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.book.title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.otherEditions", hasSize(2)))
                .andExpect(jsonPath("$.priceVerdict.label", notNullValue()))
                .andExpect(jsonPath("$.offers[0].storeName", notNullValue()));
        mockMvc.perform(get("/api/v1/editions/{id}/price-history", id).param("period", "1M"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("1M"))
                .andExpect(jsonPath("$.points", hasSize(31)))
                .andExpect(jsonPath("$.min", notNullValue()));
        mockMvc.perform(get("/api/v1/editions/{id}/price-history", id).param("period", "2Y"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v1/editions/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldServeWeeklyDropsAndBookWithEditionPrices() throws Exception {
        mockMvc.perform(get("/api/v1/home/drops").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].dropPercent", notNullValue()));

        Long bookId = editionRepository.findByIsbn13("9785601000017").orElseThrow().getBook().getId();
        mockMvc.perform(get("/api/v1/books/{id}", bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editions", hasSize(3)))
                .andExpect(jsonPath("$.editions[0].bestPrice", notNullValue()));
    }
}
