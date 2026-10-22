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
}
