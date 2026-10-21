package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.edition.EditionService;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import net.ttddyy.dsproxy.QueryCountHolder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class SearchServiceIT extends AbstractIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private EditionService editionService;

    @Autowired
    private EditionRepository editionRepository;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void shouldRunAtMostThreeQueriesPerPageAndNoneOnCacheHit() {
        SearchRequest request = new SearchRequest("и", null, null, null, false, null, null, null, 0, 5);

        QueryCountHolder.clear();
        PageResponse<SearchItem> page = searchService.search(request);
        long firstQueries = QueryCountHolder.getGrandTotal().getTotal();

        QueryCountHolder.clear();
        searchService.search(request);
        long cachedQueries = QueryCountHolder.getGrandTotal().getTotal();

        assertThat(page.content()).hasSize(5);
        assertThat(page.totalElements()).isGreaterThan(5);
        assertThat(firstQueries).isLessThanOrEqualTo(3);
        assertThat(cachedQueries).isZero();
    }

    @Test
    void shouldLoadEditionCardInFourQueries() {
        Long editionId = editionRepository.findByIsbn13("9785601000017").orElseThrow().getId();

        QueryCountHolder.clear();
        editionService.getDetail(editionId);

        assertThat(QueryCountHolder.getGrandTotal().getTotal()).isLessThanOrEqualTo(4);
    }

    @Test
    void shouldServeRepeatedSearchFromRedisWithIdenticalContent() {
        SearchRequest request = new SearchRequest("мастер маргарита", null, null, null, null, null, null,
                null, null, null);

        PageResponse<SearchItem> first = searchService.search(request);
        PageResponse<SearchItem> second = searchService.search(request);

        assertThat(first.content()).isNotEmpty();
        assertThat(second).isEqualTo(first);
        assertThat(redis.keys("search:v1:*")).hasSize(1);
    }

    @Test
    void shouldCacheSuggestionsAndEditionCardsWithVersionedKeys() {
        Long editionId = editionRepository.findByIsbn13("9785601000017").orElseThrow().getId();

        var suggestions = suggestionService.suggest("мастер");
        EditionDetailResponse card = editionService.getDetail(editionId);

        assertThat(suggestionService.suggest("Мастер")).isEqualTo(suggestions);
        assertThat(editionService.getDetail(editionId)).isEqualTo(card);
        assertThat(redis.keys("suggest:v1:*")).containsExactly("suggest:v1:мастер");
        assertThat(redis.keys("edition:v1:*")).containsExactly("edition:v1:" + editionId);
    }
}
