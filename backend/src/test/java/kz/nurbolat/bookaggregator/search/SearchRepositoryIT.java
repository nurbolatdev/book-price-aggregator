package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Sql("classpath:db/dev/catalog-seed.sql")
class SearchRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private SearchRepository searchRepository;

    @Test
    void shouldFindBookWhenQueryHasTypo() {
        List<SearchRow> rows = find(request("мастер маргарта"));

        assertThat(rows).isNotEmpty();
        assertThat(rows.get(0).title()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void shouldFindAllBooksOfAuthorWhenQueryIsAuthorSurname() {
        List<SearchRow> rows = find(withInStock(request("булгаков"), false));

        assertThat(rows).extracting(SearchRow::title)
                .contains("Мастер и Маргарита", "Собачье сердце");
    }

    @Test
    void shouldFindExactEditionWhenQueryIsIsbnWithHyphens() {
        List<SearchRow> rows = find(request("978-5-6010-0001-7"));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.title()).isEqualTo("Мастер и Маргарита");
            assertThat(row.publisher()).isEqualTo("АСТ");
        });
    }

    @Test
    void shouldIncludeEditionsWithoutOffersOnlyWhenInStockIsFalse() {
        assertThat(find(request("мертвые души"))).isEmpty();

        assertThat(find(withInStock(request("мертвые души"), false)))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.bestPrice()).isNull();
                    assertThat(row.storesCount()).isZero();
                });
    }

    @Test
    void shouldCountOnlySelectedStoresAndPriceRangeWhenFiltersAreSet() {
        SearchRequest request = new SearchRequest("толстой", List.of(StoreCode.FLIP), null,
                new BigDecimal("6000"), null, null, null, null, null, null);

        List<SearchRow> rows = find(request);

        assertThat(rows).allSatisfy(row -> {
            assertThat(row.bestStore()).isEqualTo(StoreCode.FLIP);
            assertThat(row.bestPrice()).isLessThanOrEqualTo(new BigDecimal("6000"));
            assertThat(row.storesCount()).isEqualTo(1);
        });
    }

    @Test
    void shouldFilterByBindingAndLanguage() {
        SearchRequest request = new SearchRequest("абай", null, null, null, false, List.of(Binding.HARDCOVER), List.of("kk"),
                null, null, null);

        assertThat(find(request)).isNotEmpty().extracting(SearchRow::title).containsOnly("Абай жолы");
    }

    @Test
    void shouldSortByPriceAscending() {
        List<SearchRow> rows = find(sorted("лев толстой", SearchSort.PRICE_ASC));

        assertThat(rows).hasSizeGreaterThan(1);
        assertThat(rows).extracting(SearchRow::bestPrice).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void shouldSortByStoresCountDescending() {
        List<SearchRow> rows = find(sorted("достоевский", SearchSort.STORES_DESC));

        assertThat(rows).extracting(SearchRow::storesCount).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void shouldPutDiscountedEditionsFirstWhenSortingByDiscount() {
        List<SearchRow> rows = find(sorted("булгаков", SearchSort.DISCOUNT_DESC));

        List<BigDecimal> discounts = rows.stream()
                .map(row -> row.bestOldPrice() == null || row.bestOldPrice().compareTo(row.bestPrice()) <= 0
                        ? BigDecimal.ZERO
                        : BigDecimal.ONE.subtract(row.bestPrice().divide(row.bestOldPrice(), 6, java.math.RoundingMode.HALF_UP)))
                .toList();
        assertThat(discounts).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    void shouldRankClosestMatchFirstWhenSortingByRelevance() {
        List<SearchRow> rows = find(sorted("преступление и наказание", SearchSort.RELEVANCE));

        assertThat(rows.get(0).title()).isEqualTo("Преступление и наказание");
    }

    @Test
    void shouldReturnDisjointPagesWithConsistentCount() {
        // "и" occurs in most seeded titles, enough for several pages of three
        SearchQuery firstPage = SearchQuery.from(page("и", 0));
        SearchQuery secondPage = SearchQuery.from(page("и", 1));

        List<SearchRow> page1 = searchRepository.findPage(firstPage);
        List<SearchRow> page2 = searchRepository.findPage(secondPage);
        long total = searchRepository.count(firstPage);

        assertThat(page1).hasSize(3);
        assertThat(page2).hasSize(3);
        assertThat(total).isGreaterThan(6);
        assertThat(page1).extracting(SearchRow::editionId)
                .doesNotContainAnyElementsOf(page2.stream().map(SearchRow::editionId).toList());
    }

    @Test
    void shouldReturnUpToFiveChipsPerEditionInOneQuery() {
        List<SearchRow> rows = find(request("мастер и маргарита"));
        SearchQuery query = SearchQuery.from(request("мастер и маргарита"));

        var chips = searchRepository.findOfferChips(rows.stream().map(SearchRow::editionId).toList(), query);

        assertThat(chips).isNotEmpty();
        assertThat(chips.values()).allSatisfy(list -> {
            assertThat(list).hasSizeLessThanOrEqualTo(5);
            assertThat(list.stream().map(chip -> chip.store()).distinct().count()).isEqualTo(list.size());
            assertThat(list.stream().map(chip -> chip.price()).filter(Objects::nonNull)).isNotEmpty();
        });
    }

    @Test
    void shouldSuggestTitlesByPrefixFirst() {
        var suggestions = searchRepository.findSuggestions("мас", 8);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).label()).isEqualTo("Мастер и Маргарита — Михаил Булгаков");
    }

    @Test
    void shouldSuggestBooksOfAuthorWhenQueryIsSurname() {
        var suggestions = searchRepository.findSuggestions("оруэлл", 8);

        assertThat(suggestions).extracting(s -> s.title()).contains("1984", "Скотный двор");
    }

    private List<SearchRow> find(SearchRequest request) {
        return searchRepository.findPage(SearchQuery.from(request));
    }

    private static SearchRequest request(String q) {
        return new SearchRequest(q, null, null, null, null, null, null, null, null, null);
    }

    private static SearchRequest sorted(String q, SearchSort sort) {
        return new SearchRequest(q, null, null, null, null, null, null, sort, null, null);
    }

    private static SearchRequest withInStock(SearchRequest r, boolean inStock) {
        return new SearchRequest(r.q(), r.stores(), r.minPrice(), r.maxPrice(), inStock, r.binding(),
                r.language(), r.sort(), r.page(), r.size());
    }

    private static SearchRequest page(String q, int page) {
        return new SearchRequest(q, null, null, null, false, null, null, SearchSort.PRICE_ASC, page, 3);
    }
}
