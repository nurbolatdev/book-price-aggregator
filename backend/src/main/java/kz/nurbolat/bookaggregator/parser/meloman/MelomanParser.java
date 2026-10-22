package kz.nurbolat.bookaggregator.parser.meloman;

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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static kz.nurbolat.bookaggregator.parser.core.PageValues.binding;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.clean;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.firstIsbn;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.money;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.text;
import static kz.nurbolat.bookaggregator.parser.core.PageValues.year;
import static kz.nurbolat.bookaggregator.parser.meloman.MelomanSelectors.*;

/**
 * Meloman (meloman.kz, the same catalog as marwin.kz): server-rendered Magento pages, read with Jsoup.
 * <p>
 * robots.txt (checked 2026-10-22) forbids {@code /catalogsearch/} to all robots, so the store is never
 * searched: new products are found on the book catalog pages ({@code /books/?p=N}), which are allowed,
 * and their product pages are allowed as well. The search box's own autocomplete answers nothing
 * to requests without a browser session, so it is not used either.
 * <p>
 * Product pages carry a schema.org Book (or Product) JSON-LD block and a characteristics table.
 * The price is read from the price box: the JSON-LD price is sometimes the loyalty-card price,
 * lower than what a visitor pays.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MelomanParser implements StoreParser {

    static final String BASE_URL = "https://www.meloman.kz";
    // "Форд Г.:Моя жизнь", "Сент-Экзюпери А. де: Маленький принц": an author with initials before the colon
    private static final Pattern AUTHOR_PREFIX = Pattern.compile(
            "^([^:]{1,60}?\\b[A-ZА-ЯЁӘҒҚҢӨҰҮҺІ][a-zа-яё]?\\.[^:]{0,20}):\\s*(.+)$", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String LEADING_SYMBOLS = "^[^\\p{L}\\p{N}]+";

    private final HtmlFetcher fetcher;
    private final ObjectMapper objectMapper;

    @Override
    public StoreCode store() {
        return StoreCode.MELOMAN;
    }

    @Override
    public boolean canSearch() {
        return false;
    }

    @Override
    public List<ParsedOffer> search(String query) {
        throw new UnsupportedOperationException("robots.txt of meloman.kz forbids /catalogsearch/");
    }

    @Override
    public List<ParsedOffer> browse(int page) {
        return parseCatalog(fetcher.get(StoreCode.MELOMAN, BASE_URL + "/books/?p=" + page), page);
    }

    @Override
    public Optional<ParsedOffer> fetchOffer(String url) {
        try {
            return parseProduct(fetcher.get(StoreCode.MELOMAN, url));
        } catch (PageNotFoundException e) {
            return Optional.empty();
        }
    }

    /**
     * @return empty when the page is past the last one (Magento then shows the last page again)
     */
    List<ParsedOffer> parseCatalog(Document page, int number) {
        String current = text(page, CURRENT_PAGE);
        if (current != null && !current.equals(String.valueOf(number))) {
            return List.of();
        }
        List<ParsedOffer> offers = new ArrayList<>();
        for (Element item : page.select(LIST_ITEM)) {
            Element info = item.selectFirst(ITEM_INFO);
            Element link = item.selectFirst(ITEM_LINK);
            BigDecimal price = amount(item, FINAL_PRICE);
            if (info == null || link == null || price == null) {
                continue;
            }
            String[] authorAndTitle = authorAndTitle(clean(link.text()));
            Element image = item.selectFirst(ITEM_IMAGE);
            offers.add(new ParsedOffer(
                    info.attr("data-product-id"),
                    link.absUrl("href"),
                    authorAndTitle[1],
                    authorAndTitle[0],
                    null,
                    price,
                    amount(item, OLD_PRICE),
                    true,
                    null,
                    image == null ? null : clean(image.attr("data-src")),
                    null,
                    null,
                    null,
                    clean(link.attr(ITEM_CATEGORY_ATTR))));
        }
        return offers;
    }

    /**
     * @return empty if the page is not a product page
     */
    Optional<ParsedOffer> parseProduct(Document page) {
        Element main = page.selectFirst(PRODUCT_MAIN);
        Element priceBox = main == null ? null : main.selectFirst(PRODUCT_PRICE_BOX);
        if (priceBox == null) {
            log.debug("No product data on {}", page.location());
            return Optional.empty();
        }
        JsonNode book = bookJsonLd(page);
        JsonNode offer = book == null ? null : first(book.path("offers"));
        BigDecimal price = Optional.ofNullable(amount(main, FINAL_PRICE))
                .orElseGet(() -> offer == null ? null : money(offer.path("price").asText(null)));
        if (price == null) {
            return Optional.empty();
        }

        Map<String, String> specs = specs(page);
        String heading = Optional.ofNullable(text(page, PRODUCT_TITLE)).orElse(book == null ? null : book.path("name").asText(null));
        String[] authorAndTitle = authorAndTitle(heading);
        String author = Optional.ofNullable(specs.get(SPEC_AUTHOR)).orElse(authorAndTitle[0]);
        String isbn = Optional.ofNullable(specs.get(SPEC_ISBN)).orElse(book == null ? null : book.path("isbn").asText(null));
        String publisher = Optional.ofNullable(specs.get(SPEC_PUBLISHER))
                .orElse(book == null ? null : book.path("publisher").asText(null));
        Integer year = Optional.ofNullable(year(specs.get(SPEC_YEAR)))
                .orElse(book == null ? null : year(book.path("datePublished").asText(null)));

        return Optional.of(new ParsedOffer(
                priceBox.attr("data-product-id"),
                productUrl(page),
                authorAndTitle[1],
                author,
                firstIsbn(isbn),
                price,
                amount(main, OLD_PRICE),
                inStock(main, offer),
                null,
                book == null ? null : book.path("image").asText(null),
                publisher,
                year,
                binding(specs.get(SPEC_BINDING)),
                category(page)));
    }

    // Splits "Форд Г.:Моя жизнь. Мои достижения" into author and title; titles without an author stay whole
    static String[] authorAndTitle(String heading) {
        if (heading == null) {
            return new String[]{null, null};
        }
        Matcher matcher = AUTHOR_PREFIX.matcher(heading);
        return matcher.matches()
                ? new String[]{matcher.group(1).strip(), matcher.group(2).strip()}
                : new String[]{null, heading};
    }

    private static boolean inStock(Element main, JsonNode offer) {
        if (offer != null && offer.hasNonNull("availability")) {
            return offer.path("availability").asText().endsWith("/InStock");
        }
        Element stock = main.selectFirst(PRODUCT_STOCK);
        return stock != null && stock.hasClass("available");
    }

    private JsonNode bookJsonLd(Document page) {
        for (Element script : page.select(JSON_LD)) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                String type = node.path("@type").asText();
                if ("Product".equals(type) || "Book".equals(type)) {
                    return node;
                }
                if ("WebPage".equals(type) && node.has("mainEntity")) {
                    return node.path("mainEntity");
                }
            } catch (JsonProcessingException e) {
                log.debug("Unreadable JSON-LD on {}: {}", page.location(), e.getOriginalMessage());
            }
        }
        return null;
    }

    // Breadcrumb names without their emoji: "Книги/Публицистика. Биографии. Мемуары./Тру-крайм"
    private String category(Document page) {
        for (Element script : page.select(JSON_LD)) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                if ("BreadcrumbList".equals(node.path("@type").asText())) {
                    List<JsonNode> items = StreamSupport.stream(node.path("itemListElement").spliterator(), false).toList();
                    // The last crumb is the product itself
                    String path = items.stream().limit(Math.max(0, items.size() - 1))
                            .map(item -> item.path("item").path("name").asText("").replaceAll(LEADING_SYMBOLS, ""))
                            .filter(name -> !name.isBlank())
                            .collect(Collectors.joining("/"));
                    return path.isEmpty() ? null : path;
                }
            } catch (JsonProcessingException e) {
                log.debug("Unreadable JSON-LD on {}: {}", page.location(), e.getOriginalMessage());
            }
        }
        return null;
    }

    private static Map<String, String> specs(Document page) {
        Map<String, String> specs = new HashMap<>();
        for (Element cell : page.select(SPEC_CELL)) {
            String value = clean(cell.text());
            if (value != null) {
                specs.putIfAbsent(cell.attr("data-th").strip(), value);
            }
        }
        return specs;
    }

    private static String productUrl(Document page) {
        Element canonical = page.selectFirst(CANONICAL);
        return canonical != null && !canonical.attr("href").isBlank() ? canonical.attr("href") : page.location();
    }

    private static BigDecimal amount(Element root, String selector) {
        Element element = root.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String amount = element.attr(PRICE_AMOUNT_ATTR);
        try {
            // "3750.0000" on some cards: whole tenge like on other stores
            return new BigDecimal(amount).setScale(0, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static JsonNode first(JsonNode node) {
        return node.isArray() ? (node.isEmpty() ? null : node.get(0)) : (node.isMissingNode() ? null : node);
    }
}
