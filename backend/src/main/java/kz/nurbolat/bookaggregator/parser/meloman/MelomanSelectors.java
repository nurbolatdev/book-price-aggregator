package kz.nurbolat.bookaggregator.parser.meloman;

/**
 * CSS selectors of meloman.kz pages (Magento, checked 2026-10-22). When Meloman changes its markup,
 * update them together with the fixtures in src/test/resources/fixtures/meloman.
 */
final class MelomanSelectors {

    private MelomanSelectors() {
    }

    // Catalog page: one <li class="product-item"> per product
    static final String LIST_ITEM = "li.product-item";
    static final String ITEM_INFO = ".product-item-info[data-product-id]";
    static final String ITEM_LINK = "a.product-item-link";
    static final String ITEM_CATEGORY_ATTR = "data-category";
    static final String ITEM_IMAGE = "img.product-image-photo";
    static final String CURRENT_PAGE = ".pages .item.current strong.page span:last-child";

    // Both pages: prices are attributes of the price box, the text only formats them
    static final String FINAL_PRICE = "[data-price-type=finalPrice][data-price-amount]";
    static final String OLD_PRICE = "[data-price-type=oldPrice][data-price-amount]";
    static final String PRICE_AMOUNT_ATTR = "data-price-amount";

    // Product page
    static final String JSON_LD = "script[type=application/ld+json]";
    static final String PRODUCT_MAIN = ".product-info-main";
    static final String PRODUCT_PRICE_BOX = ".product-info-price [data-role=priceBox][data-product-id]";
    static final String PRODUCT_TITLE = "h1.page-title .base";
    static final String PRODUCT_STOCK = ".stock";
    static final String CANONICAL = "link[rel=canonical]";
    // Characteristics table: <td class="col data" data-th="Издательство">АСТ</td>
    static final String SPEC_CELL = "td.col.data[data-th]";

    static final String SPEC_AUTHOR = "Автор";
    static final String SPEC_PUBLISHER = "Издательство";
    static final String SPEC_BINDING = "Переплет";
    static final String SPEC_YEAR = "Год издания";
    static final String SPEC_ISBN = "ISBN";
}
