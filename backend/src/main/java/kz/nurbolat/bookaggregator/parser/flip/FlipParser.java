package kz.nurbolat.bookaggregator.parser.flip;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.nurbolat.bookaggregator.common.text.IsbnUtils;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.parser.core.HtmlFetcher;
import kz.nurbolat.bookaggregator.parser.core.PageNotFoundException;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.parser.core.StoreParser;
import kz.nurbolat.bookaggregator.store.StoreCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static kz.nurbolat.bookaggregator.parser.flip.FlipSelectors.*;

/**
 * Flip.kz: server-rendered HTML, read with Jsoup.
 * <p>
 * robots.txt (checked 2026-10-22) has groups only for Yandex and Bingbot, so nothing is restricted
 * for other agents. The parser still requests only what those groups allow: the first search page
 * ({@code /search?search=}, without page, filter or order parameters) and product pages
 * ({@code /catalog?prod=}).
 * <p>
 * Search cards carry price, availability, title, binding, year and publisher, but no author or ISBN;
 * product pages add a schema.org/Product JSON-LD block (price, availability, image) and a
 * characteristics table (ISBN, publisher, binding, year).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlipParser implements StoreParser {

    static final String BASE_URL = "https://www.flip.kz";
    private static final Pattern PRODUCT_ID = Pattern.compile("prod=(\\d+)");
    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");

    private final HtmlFetcher fetcher;
    private final ObjectMapper objectMapper;

    @Override
    public StoreCode store() {
        return StoreCode.FLIP;
    }

    @Override
    public List<ParsedOffer> search(String query) {
        String url = BASE_URL + "/search?search=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        return parseSearch(fetcher.get(StoreCode.FLIP, url));
    }

    @Override
    public Optional<ParsedOffer> fetchOffer(String url) {
        try {
            return parseProduct(fetcher.get(StoreCode.FLIP, url));
        } catch (PageNotFoundException e) {
            return Optional.empty();
        }
    }

    List<ParsedOffer> parseSearch(Document page) {
        List<ParsedOffer> offers = new ArrayList<>();
        for (Element card : page.select(SEARCH_CARD)) {
            String id = productId(card.attr("href"));
            BigDecimal price = money(text(card, CARD_PRICE));
            if (id == null || price == null) {
                continue;
            }
            Elements description = card.select(CARD_DESCRIPTION);
            String format = description.isEmpty() ? null : description.get(0).text();
            String publisherLine = description.size() < 2 ? null : description.get(1).text();
            Element data = card.selectFirst(CARD_DATA);
            Element image = card.selectFirst(CARD_IMAGE);

            offers.add(new ParsedOffer(
                    id,
                    productUrl(id),
                    text(card, CARD_TITLE),
                    null,
                    null,
                    price,
                    money(text(card, CARD_OLD_PRICE)),
                    data != null && "1".equals(data.attr(CARD_AVAILABLE_ATTR)),
                    text(card, CARD_DELIVERY),
                    image == null ? null : absolute(image.attr("src")),
                    firstListItem(publisherLine),
                    year(format),
                    binding(format)));
        }
        return offers;
    }

    /**
     * @return empty if the page is not a product (e.g. Flip redirected to another page)
     */
    Optional<ParsedOffer> parseProduct(Document page) {
        JsonNode product = productJsonLd(page);
        String id = product != null ? product.path("sku").asText(null) : null;
        if (id == null) {
            id = productId(page.location());
        }
        if (product == null || id == null) {
            log.debug("No product data on {}", page.location());
            return Optional.empty();
        }

        JsonNode offer = product.path("offers");
        BigDecimal price = Optional.ofNullable(money(offer.path("price").asText(null)))
                .orElseGet(() -> money(text(page, PRODUCT_PRICE)));
        if (price == null) {
            return Optional.empty();
        }
        Map<String, String> specs = specs(page);
        Element availability = page.selectFirst(PRODUCT_AVAILABLE);
        boolean inStock = offer.hasNonNull("availability")
                ? offer.path("availability").asText().endsWith("/InStock")
                : availability != null && "1".equals(availability.attr("data-available"));
        String title = Optional.ofNullable(product.path("name").asText(null)).orElseGet(() -> text(page, PRODUCT_TITLE));

        return Optional.of(new ParsedOffer(
                id,
                productUrl(id),
                title,
                authors(page),
                isbn(specs.get(SPEC_ISBN)),
                price,
                money(text(page, PRODUCT_OLD_PRICE)),
                inStock,
                text(page, PRODUCT_DELIVERY),
                image(product),
                Optional.ofNullable(specs.get(SPEC_PUBLISHER)).orElse(product.path("brand").path("name").asText(null)),
                year(specs.get(SPEC_YEAR)),
                binding(specs.get(SPEC_BINDING))));
    }

    private JsonNode productJsonLd(Document page) {
        for (Element script : page.select(JSON_LD)) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                if ("Product".equals(node.path("@type").asText())) {
                    return node;
                }
            } catch (JsonProcessingException e) {
                log.debug("Unreadable JSON-LD on {}: {}", page.location(), e.getOriginalMessage());
            }
        }
        return null;
    }

    private static Map<String, String> specs(Document page) {
        Map<String, String> specs = new HashMap<>();
        for (Element row : page.select(SPEC_ROW)) {
            Elements cells = row.select(SPEC_CELL);
            specs.putIfAbsent(cells.get(0).text().strip(), cells.get(1).text().strip());
        }
        return specs;
    }

    private static String authors(Document page) {
        List<String> names = page.select(PRODUCT_AUTHORS).eachText().stream().distinct().toList();
        return names.isEmpty() ? null : String.join(", ", names);
    }

    // "978-5-17-056954-0, 978-5-271-27312-4": the first valid one
    private static String isbn(String cell) {
        if (cell == null) {
            return null;
        }
        for (String candidate : cell.split("[,;]")) {
            String isbn13 = IsbnUtils.normalize(candidate);
            if (isbn13 != null) {
                return isbn13;
            }
        }
        return null;
    }

    private static String image(JsonNode product) {
        JsonNode image = product.path("image");
        String url = image.isArray() ? image.path(0).asText(null) : image.asText(null);
        return url == null ? null : absolute(url);
    }

    static Binding binding(String text) {
        if (text == null) {
            return Binding.UNKNOWN;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("тверд") || lower.contains("твёрд")) {
            return Binding.HARDCOVER;
        }
        if (lower.contains("мягк")) {
            return Binding.PAPERBACK;
        }
        return Binding.UNKNOWN;
    }

    static Integer year(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /** Tenge amounts like "2 021 ₸" (with narrow no-break spaces); {@code null} if there are no digits. */
    static BigDecimal money(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("\\D", "");
        return digits.isEmpty() ? null : new BigDecimal(digits);
    }

    // "МИФ (Манн, Иванов и Фербер), Вечные истории" -> "МИФ (Манн, Иванов и Фербер)"
    static String firstListItem(String text) {
        if (text == null) {
            return null;
        }
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
            } else if (c == ',' && depth == 0) {
                return text.substring(0, i).strip();
            }
        }
        return text.strip();
    }

    private static String productId(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = PRODUCT_ID.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String productUrl(String id) {
        return BASE_URL + "/catalog?prod=" + id;
    }

    private static String absolute(String src) {
        return src.startsWith("//") ? "https:" + src : src;
    }

    // Element text with no-break spaces turned into plain ones; null when missing or blank
    private static String text(Element root, String selector) {
        Element element = root.selectFirst(selector);
        if (element == null) {
            return null;
        }
        String text = element.text().replace(' ', ' ').replace(' ', ' ').replaceAll("\\s+", " ").strip();
        return text.isEmpty() ? null : text;
    }
}
