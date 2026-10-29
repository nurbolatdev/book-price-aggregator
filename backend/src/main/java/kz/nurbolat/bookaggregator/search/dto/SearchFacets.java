package kz.nurbolat.bookaggregator.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.util.List;

/**
 * Counts of editions for the filter lists. Each count is the total that the search would return
 * with the same filters and only this value selected.
 */
public record SearchFacets(List<StoreFacet> stores, List<ValueFacet> bindings, List<ValueFacet> languages) {

    public record StoreFacet(StoreCode code, long count) {
    }

    @Schema(description = "Переплёт (HARDCOVER, PAPERBACK) или язык (ru, kk, en)")
    public record ValueFacet(String value, long count) {
    }
}
