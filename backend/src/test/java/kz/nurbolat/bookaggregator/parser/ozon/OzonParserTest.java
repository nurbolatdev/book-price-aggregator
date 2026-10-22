package kz.nurbolat.bookaggregator.parser.ozon;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.parser.core.HtmlFetcher;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Parses saved ozon.kz pages (src/test/resources/fixtures/ozon); no network.
 */
class OzonParserTest {

    private final OzonParser parser = new OzonParser(mock(HtmlFetcher.class), new ObjectMapper());

    @Test
    void shouldReadAllTilesOfCatalogPage() {
        List<ParsedOffer> offers = parser.parseCatalog(
                fixture("category-knigi-page-1.html", "https://ozon.kz/category/knigi-16500/?page=1"));

        assertThat(offers).hasSize(48);
        assertThat(offers.get(0)).isEqualTo(new ParsedOffer(
                "4915871966",
                "https://ozon.kz/product/znatok-excel-tablitsy-i-formuly-bez-golovnoy-boli-leonov-vasiliy-4915871966/",
                "Знаток Excel. Таблицы и формулы без головной боли",
                "Леонов Василий",
                null,
                new BigDecimal("3083"),
                new BigDecimal("5317"),
                true,
                null,
                null,
                null,
                null,
                null));
        // a tile without the " | author" part and without a crossed-out price
        assertThat(offers).filteredOn(offer -> offer.externalId().equals("5835994396")).singleElement()
                .satisfies(offer -> {
                    assertThat(offer.title()).isEqualTo("ОГЭ 2027 География 32 варианта");
                    assertThat(offer.author()).isNull();
                    assertThat(offer.price()).isEqualByComparingTo("3762");
                    assertThat(offer.oldPrice()).isNull();
                });
    }

    @Test
    void shouldReturnNothingForPageWithoutTiles() {
        Document empty = Jsoup.parse("<html><body></body></html>", "https://ozon.kz/category/knigi-16500/?page=999");

        assertThat(parser.parseCatalog(empty)).isEmpty();
    }

    @Test
    void shouldReadDiscountedProductFromPriceWidgetAndCharacteristics() {
        ParsedOffer offer = product("product-273872197-discount.html",
                "https://ozon.kz/product/100-i-1-stihotvorenie-kotoroe-nado-znat-pushkin-aleksandr-sergeevich-gumilev-nikolay-stepanovich-273872197/");

        assertThat(offer).isEqualTo(new ParsedOffer(
                "273872197",
                "https://ozon.kz/product/100-i-1-stihotvorenie-kotoroe-nado-znat-pushkin-aleksandr-sergeevich-gumilev-nikolay-stepanovich-273872197/",
                "100 и 1 стихотворение, которое надо знать.",
                "Пушкин Александр Сергеевич, Гумилев Николай Степанович",
                "9785998817212",
                new BigDecimal("930"),
                new BigDecimal("2584"),
                true,
                null,
                "https://ir-20.ozone.ru/s3/multimedia-6/6041147322.jpg",
                "РГ-Пресс",
                2026,
                Binding.PAPERBACK,
                null));
    }

    @Test
    void shouldReadProductWithoutDiscount() {
        ParsedOffer offer = product("product-5835994396.html", "https://ozon.kz/product/oge-2027-geografiya-32-varianta-5835994396/");

        assertThat(offer.externalId()).isEqualTo("5835994396");
        assertThat(offer.title()).isEqualTo("ОГЭ 2027 География 32 варианта");
        assertThat(offer.isbn13()).isEqualTo("9785377230236");
        assertThat(offer.price()).isEqualByComparingTo("3762");
        assertThat(offer.oldPrice()).isNull();
        assertThat(offer.inStock()).isTrue();
        assertThat(offer.year()).isEqualTo(2027);
        assertThat(offer.binding()).isEqualTo(Binding.PAPERBACK);
    }

    @Test
    void shouldLeaveIsbnEmptyWhenPageLoadsCharacteristicsLater() {
        ParsedOffer offer = product("product-313345021-no-characteristics.html",
                "https://ozon.kz/product/zelenaya-milya-king-stiven-313345021/");

        assertThat(offer.title()).isEqualTo("Зеленая миля");
        assertThat(offer.author()).isEqualTo("Кинг Стивен");
        assertThat(offer.isbn13()).isNull();
        assertThat(offer.year()).isNull();
        assertThat(offer.binding()).isEqualTo(Binding.UNKNOWN);
        assertThat(offer.price()).isEqualByComparingTo("2443");
        assertThat(offer.oldPrice()).isEqualByComparingTo("5646");
    }

    @Test
    void shouldMarkOutOfStockWhenPriceWidgetSaysSo() {
        ParsedOffer offer = product("product-5835994396-out-of-stock-edited.html",
                "https://ozon.kz/product/oge-2027-geografiya-32-varianta-5835994396/");

        assertThat(offer.inStock()).isFalse();
    }

    @Test
    void shouldReturnNothingForPageWithoutProduct() {
        Document home = Jsoup.parse("<html><body><h1>Ozon</h1></body></html>", "https://ozon.kz/");

        assertThat(parser.parseProduct(home)).isEmpty();
    }

    @Test
    void shouldNeverSearchBecauseRobotsTxtForbidsIt() {
        assertThat(parser.canSearch()).isFalse();
        assertThatThrownBy(() -> parser.search("Зеленая миля")).isInstanceOf(UnsupportedOperationException.class);
    }

    private ParsedOffer product(String file, String url) {
        return parser.parseProduct(fixture(file, url)).orElseThrow();
    }

    private static Document fixture(String file, String url) {
        try (InputStream in = OzonParserTest.class.getResourceAsStream("/fixtures/ozon/" + file)) {
            return Jsoup.parse(in, "UTF-8", url);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
