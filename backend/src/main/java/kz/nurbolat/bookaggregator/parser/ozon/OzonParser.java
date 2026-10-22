package kz.nurbolat.bookaggregator.parser.ozon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.nurbolat.bookaggregator.parser.core.HtmlFetcher;
import kz.nurbolat.bookaggregator.parser.core.PageNotFoundException;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.parser.core.StoreParser;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static kz.nurbolat.bookaggregator.parser.core.PageValues.binding;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.clean;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.firstIsbn;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.money;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.year;
import static kz.nurbolat.bookaggregator.parser.ozon.OzonSelectors.*;

/**
 * Ozon (ozon.kz): the pages are rendered on the server with their data, so plain HTTP is enough
 * (checked 2026-10-22: the antibot layer lets the parser through; if it ever answers 403 or a captcha,
 * the fetcher stops the store as for any other).
 * <p>
 * robots.txt forbids {@code /search} to all robots, so new products are found on the pages of the
 * book category ({@code /category/knigi-16500/?page=N}, explicitly allowed); product pages are allowed.
 * <p>
 * Data comes from JSON-LD (price, availability, publisher as "brand") and the JSON state of page widgets:
 * the price widget (price, crossed-out price, availability), the heading ("Title | Authors") and the
 * characteristics (ISBN, year, cover). Some product pages load their characteristics later by script;
 * those offers have no ISBN.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OzonParser implements StoreParser {

    static final String BASE_URL = "https://ozon.kz";
    static final String BOOKS_CATEGORY = BASE_URL + "/category/knigi-16500/";
    // "Зеленая миля | Кинг Стивен"
    private static final String AUTHOR_SEPARATOR = " | ";

    private final HtmlFetcher fetcher;
    private final ObjectMapper objectMapper;

    @Override
    public StoreCode store() {
        return StoreCode.OZON;
    }

    @Override
    public boolean canSearch() {
        return false;
    }

    @Override
    public List<ParsedOffer> search(String query) {
        throw new UnsupportedOperationException("robots.txt of ozon.kz forbids /search");
    }

    @Override
    public List<ParsedOffer> browse(int page) {
        return parseCatalog(fetcher.get(StoreCode.OZON, BOOKS_CATEGORY + "?page=" + page));
    }

    @Override
    public Optional<ParsedOffer> fetchOffer(String url) {
        try {
            return parseProduct(fetcher.get(StoreCode.OZON, url));
        } catch (PageNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * @return empty past the last page (the grid has no tiles)
     */
    List<ParsedOffer> parseCatalog(Document page) {
        JsonNode grid = state(page, TILE_GRID);
        if (grid == null) {
            return List.of();
        }
        List<ParsedOffer> offers = new ArrayList<>();
        for (JsonNode tile : grid.path("items")) {
            String sku = tile.path("sku").asText(null);
            String link = tile.path("action").path("link").asText(null);
            BigDecimal price = null;
            BigDecimal oldPrice = null;
            String name = null;
            for (JsonNode part : tile.path("mainState")) {
                if ("priceV2".equals(part.path("type").asText())) {
                    for (JsonNode text : part.path("priceV2").path("price")) {
                        String value = text.path("text").asText("");
                        // "771 ₸ × 4 мес" is an instalment, not the price
                        if (value.contains("×")) {
                            continue;
                        }
                        switch (text.path("textStyle").asText()) {
                            case TILE_PRICE_STYLE -> price = money(value);
                            case TILE_OLD_PRICE_STYLE -> oldPrice = money(value);
                            default -> { }
                        }
                    }
                } else if (TILE_NAME_ID.equals(part.path("id").asText())) {
                    name = clean(part.path("textDS").path("text").asText(null));
                }
            }
            if (sku == null || link == null || price == null) {
                continue;
            }
            String[] titleAndAuthor = titleAndAuthor(name);
            offers.add(new ParsedOffer(sku, productUrl(link), titleAndAuthor[0], titleAndAuthor[1], null, price,
                    oldPrice, true, null, null, null, null, null));
        }
        return offers;
    }

    /**
     * @return empty if the page is not a product page
     */
    Optional<ParsedOffer> parseProduct(Document page) {
        JsonNode product = productJsonLd(page);
        JsonNode offer = product == null ? null : product.path("offers");
        JsonNode priceState = state(page, PRICE);
        if (offer == null && priceState == null) {
            log.debug("No product data on {}", page.location());
            return Optional.empty();
        }
        String sku = Optional.ofNullable(product == null ? null : product.path("sku").asText(null))
                .orElseGet(() -> skuFromUrl(page.location()));
        BigDecimal price = Optional.ofNullable(priceState == null ? null : money(priceState.path("price").asText(null)))
                .orElseGet(() -> offer == null ? null : money(offer.path("price").asText(null)));
        if (sku == null || price == null) {
            return Optional.empty();
        }

        JsonNode heading = state(page, HEADING);
        String[] titleAndAuthor = titleAndAuthor(Optional.ofNullable(heading == null ? null : clean(heading.path("title").asText(null)))
                .orElseGet(() -> product == null ? null : clean(product.path("name").asText(null))));
        Map<String, String> specs = characteristics(page);
        String author = Optional.ofNullable(titleAndAuthor[1]).orElse(specs.get(SPEC_WRITER));
        boolean inStock = priceState != null && priceState.has("isAvailable")
                ? priceState.path("isAvailable").asBoolean()
                : offer != null && offer.path("availability").asText().endsWith("/InStock");

        return Optional.of(new ParsedOffer(
                sku,
                Optional.ofNullable(offer == null ? null : offer.path("url").asText(null)).orElse(page.location()),
                titleAndAuthor[0],
                author,
                firstIsbn(specs.get(SPEC_ISBN)),
                price,
                priceState == null ? null : money(priceState.path("originalPrice").asText(null)),
                inStock,
                null,
                image(page),
                product == null ? null : clean(product.path("brand").asText(null)),
                year(specs.get(SPEC_YEAR)),
                binding(specs.get(SPEC_COVER)),
                null));
    }

    // "Зеленая миля | Кинг Стивен" -> title and author; a name without the separator is all title
    static String[] titleAndAuthor(String name) {
        if (name == null) {
            return new String[]{null, null};
        }
        int separator = name.lastIndexOf(AUTHOR_SEPARATOR);
        if (separator < 0) {
            return new String[]{name, null};
        }
        String author = clean(name.substring(separator + AUTHOR_SEPARATOR.length()));
        return new String[]{name.substring(0, separator).strip(), author};
    }

    // The product block: the one with offers (several JSON-LD blocks describe the same product)
    private JsonNode productJsonLd(Document page) {
        for (JsonNode node : jsonLd(page)) {
            if ("Product".equals(node.path("@type").asText()) && node.has("offers")) {
                return node;
            }
        }
        return null;
    }

    private String image(Document page) {
        for (JsonNode node : jsonLd(page)) {
            String image = node.path("image").asText(null);
            if (image != null) {
                return image;
            }
        }
        return null;
    }

    private List<JsonNode> jsonLd(Document page) {
        List<JsonNode> nodes = new ArrayList<>();
        for (Element script : page.select(JSON_LD)) {
            try {
                nodes.add(objectMapper.readTree(script.data()));
            } catch (JsonProcessingException e) {
                log.debug("Unreadable JSON-LD on {}: {}", page.location(), e.getOriginalMessage());
            }
        }
        return nodes;
    }

    private Map<String, String> characteristics(Document page) {
        Map<String, String> specs = new HashMap<>();
        JsonNode state = state(page, CHARACTERISTICS);
        if (state == null) {
            return specs;
        }
        for (JsonNode group : state.path("characteristics")) {
            for (String part : List.of("short", "long")) {
                for (JsonNode characteristic : group.path(part)) {
                    String values = StreamSupport.stream(characteristic.path("values").spliterator(), false)
                            .map(value -> value.path("text").asText(""))
                            .collect(Collectors.joining(", "));
                    specs.putIfAbsent(characteristic.path("key").asText(), clean(values));
                }
            }
        }
        return specs;
    }

    /**
     * State of the first widget of that name that has data; {@code null} if the page has none.
     */
    private JsonNode state(Document page, String widget) {
        for (Element element : page.select("div[id^=state-" + widget + "-][" + STATE_ATTR + "]")) {
            try {
                JsonNode node = objectMapper.readTree(element.attr(STATE_ATTR));
                if (!node.isEmpty()) {
                    return node;
                }
            } catch (JsonProcessingException e) {
                log.debug("Unreadable {} state on {}: {}", widget, page.location(), e.getOriginalMessage());
            }
        }
        return null;
    }

    // Tile links carry tracking parameters; the product page itself is the path
    private static String productUrl(String link) {
        return BASE_URL + URI.create(link.startsWith("http") ? link : BASE_URL + link).getRawPath();
    }

    // "/product/zelenaya-milya-king-stiven-313345021/" -> "313345021"
    private static String skuFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String path = URI.create(url).getRawPath().replaceAll("/+$", "");
        int dash = path.lastIndexOf('-');
        String sku = dash < 0 ? null : path.substring(dash + 1);
        return sku != null && sku.chars().allMatch(Character::isDigit) && !sku.isEmpty() ? sku : null;
    }
}
