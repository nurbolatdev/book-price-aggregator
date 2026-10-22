package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.store.StoreCode;

import java.util.List;
import java.util.Optional;

/**
 * Extracts offers from the pages of one store. Never writes to the database;
 * all network access goes through {@link HtmlFetcher}.
 */
public interface StoreParser {

    StoreCode store();

    /**
     * Offers from the store's search page. Search pages may lack details (ISBN, author);
     * {@link #fetchOffer(String)} returns the full data.
     */
    List<ParsedOffer> search(String query);

    /**
     * Offer from a product page, or empty if the product no longer exists.
     */
    Optional<ParsedOffer> fetchOffer(String url);

    /**
     * {@code false} when the store's robots.txt forbids its search pages; new offers are then
     * found by {@link #browse(int)} instead of {@link #search(String)}.
     */
    default boolean canSearch() {
        return true;
    }

    /**
     * Offers on one page of the store's book catalog, for stores that cannot be searched.
     *
     * @param page 1 for the first page
     * @return empty past the last page
     */
    default List<ParsedOffer> browse(int page) {
        throw new UnsupportedOperationException(store() + " is searched, not browsed");
    }
}
