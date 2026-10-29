package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.text.Hashing;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.dto.FacetsRequest;
import kz.nurbolat.bookaggregator.search.dto.SearchFacets;
import kz.nurbolat.bookaggregator.search.dto.SearchFacets.StoreFacet;
import kz.nurbolat.bookaggregator.search.dto.SearchFacets.ValueFacet;
import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Facet counts for the filter panel. A value's count ignores the selection of its own filter, so the
 * user sees what choosing it would give. Cached in Redis for a minute.
 */
@Slf4j
@Service
public class SearchFacetsService {

    static final List<String> LANGUAGES = List.of("ru", "kk", "en");
    static final List<Binding> BINDINGS = List.of(Binding.HARDCOVER, Binding.PAPERBACK);

    private final SearchFacetsRepository repository;
    private final StoreRepository storeRepository;
    private final Cache facetsCache;

    public SearchFacetsService(SearchFacetsRepository repository, StoreRepository storeRepository,
                               CacheManager cacheManager) {
        this.repository = repository;
        this.storeRepository = storeRepository;
        this.facetsCache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.SEARCH_FACETS));
    }

    public SearchFacets facets(FacetsRequest request) {
        SearchQuery query = SearchQuery.from(request.toSearchRequest());
        List<StoreCode> stores = storeRepository.findAllByOrderByIdAsc().stream()
                .filter(Store::isEnabled)
                .map(Store::getCode)
                .toList();
        if (query.isBlank()) {
            return build(stores, Map.of(), Map.of(), Map.of());
        }
        String key = Hashing.sha1Hex(SearchService.filterKey(query) + "|" + stores);
        try {
            return facetsCache.get(key, () -> compute(query, stores));
        } catch (DataAccessException e) {
            // Redis is unavailable: answer from the database
            log.warn("Facets cache unavailable: {}", e.getMessage());
            return compute(query, stores);
        }
    }

    private SearchFacets compute(SearchQuery query, List<StoreCode> stores) {
        Map<StoreCode, Long> storeCounts = new java.util.EnumMap<>(StoreCode.class);
        for (StoreCode store : stores) {
            storeCounts.put(store, repository.count(query.withStores(List.of(store))));
        }
        return build(stores, storeCounts,
                repository.countByBinding(query.withBindings(List.of())),
                repository.countByLanguage(query.withLanguages(List.of())));
    }

    private static SearchFacets build(List<StoreCode> stores, Map<StoreCode, Long> storeCounts,
                                      Map<String, Long> bindingCounts, Map<String, Long> languageCounts) {
        return new SearchFacets(
                stores.stream().map(code -> new StoreFacet(code, storeCounts.getOrDefault(code, 0L))).toList(),
                BINDINGS.stream().map(b -> new ValueFacet(b.name(), bindingCounts.getOrDefault(b.name(), 0L))).toList(),
                LANGUAGES.stream().map(l -> new ValueFacet(l, languageCounts.getOrDefault(l, 0L))).toList());
    }
}
