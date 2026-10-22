package kz.nurbolat.bookaggregator.parser.ozon;

/**
 * Where ozon.kz keeps its data (checked 2026-10-22): JSON-LD and the JSON state of page widgets,
 * {@code <div id="state-<widget>-<id>-default-1" data-state='{...}'>}. When Ozon changes its pages,
 * update these together with the fixtures in src/test/resources/fixtures/ozon.
 */
final class OzonSelectors {

    private OzonSelectors() {
    }

    static final String JSON_LD = "script[type=application/ld+json]";
    static final String STATE_ATTR = "data-state";

    // Widgets whose state is read
    static final String TILE_GRID = "tileGridDesktop";
    static final String PRICE = "webPrice";
    static final String HEADING = "webProductHeading";
    static final String CHARACTERISTICS = "webCharacteristics";

    // Tile price parts
    static final String TILE_PRICE_STYLE = "PRICE";
    static final String TILE_OLD_PRICE_STYLE = "ORIGINAL_PRICE";
    static final String TILE_NAME_ID = "name";

    // Characteristic keys
    static final String SPEC_ISBN = "ISBN";
    static final String SPEC_YEAR = "ReleaseYear";
    static final String SPEC_COVER = "CoverType";
    static final String SPEC_WRITER = "Writer";
}
