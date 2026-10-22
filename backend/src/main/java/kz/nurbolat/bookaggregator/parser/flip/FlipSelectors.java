package kz.nurbolat.bookaggregator.parser.flip;

/**
 * CSS selectors of flip.kz pages (checked 2026-10-22). When Flip changes its markup, update them
 * together with the fixtures in src/test/resources/fixtures/flip.
 */
final class FlipSelectors {

    private FlipSelectors() {
    }

    // Search results: one <a class="product"> per card
    static final String SEARCH_CARD = "a.product[href*=catalog?prod=]";
    static final String CARD_DATA = ".product-data";
    static final String CARD_AVAILABLE_ATTR = "data-available";
    static final String CARD_PRICE = ".price > span:not(.old)";
    static final String CARD_OLD_PRICE = ".price > span.old";
    static final String CARD_TITLE = ".title";
    // "твердый переплет, 2023" and "Publisher, Series"
    static final String CARD_DESCRIPTION = ".description > span";
    static final String CARD_DELIVERY = ".delivery-day-text-flex";
    static final String CARD_IMAGE = "img.image";

    // Product page
    static final String JSON_LD = "script[type=application/ld+json]";
    static final String PRODUCT_TITLE = "h1";
    static final String PRODUCT_AUTHORS = "h1 + p a[href*=cat=people]";
    static final String PRODUCT_PRICE = ".right-to-cart .new-price";
    static final String PRODUCT_OLD_PRICE = ".right-to-cart .old-price";
    static final String PRODUCT_AVAILABLE = ".right-to-cart[data-available]";
    static final String PRODUCT_DELIVERY = ".right-to-cart .delivery-day-text-flex";
    // Characteristics table: <div class="row"><div class="cell">Name</div><div class="cell">Value</div></div>
    static final String SPEC_ROW = "div.row:has(> div.cell + div.cell)";
    static final String SPEC_CELL = "> div.cell";

    static final String SPEC_PUBLISHER = "Издательство";
    static final String SPEC_BINDING = "Переплет";
    static final String SPEC_YEAR = "Дата выхода";
    static final String SPEC_ISBN = "ISBN";
}
