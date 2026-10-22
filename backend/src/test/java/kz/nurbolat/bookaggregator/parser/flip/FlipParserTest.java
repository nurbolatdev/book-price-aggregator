package kz.nurbolat.bookaggregator.parser.flip;

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
import static org.mockito.Mockito.mock;

/**
 * Parses saved flip.kz pages (src/test/resources/fixtures/flip); no network.
 */
class FlipParserTest {

    private final FlipParser parser = new FlipParser(mock(HtmlFetcher.class), new ObjectMapper());

    @Test
    void shouldReadAllCardsOfSearchPage() {
        List<ParsedOffer> offers = parser.parseSearch(fixture("search-master-i-margarita.html",
                "https://www.flip.kz/search?search=мастер+и+маргарита"));

        assertThat(offers).hasSize(60);
        assertThat(offers.get(0)).isEqualTo(new ParsedOffer(
                "2854608",
                "https://www.flip.kz/catalog?prod=2854608",
                "Мастер и Маргарита",
                null,
                null,
                new BigDecimal("2722"),
                new BigDecimal("6048"),
                true,
                "Доставим бесплатно в понедельник, 26 октября",
                "https://s.f.kz/prod/2855/2854608_550.jpg",
                "МИФ (Манн, Иванов и Фербер)",
                2023,
                Binding.HARDCOVER));
        assertThat(offers).allSatisfy(offer -> {
            assertThat(offer.externalId()).isNotBlank();
            assertThat(offer.price()).isPositive();
            assertThat(offer.title()).isNotBlank();
        });
        assertThat(offers).extracting(ParsedOffer::binding).contains(Binding.PAPERBACK);
    }

    @Test
    void shouldReadRegularProductPage() {
        ParsedOffer offer = product("product-1269662.html", "1269662");

        assertThat(offer).isEqualTo(new ParsedOffer(
                "1269662",
                "https://www.flip.kz/catalog?prod=1269662",
                "Мастер и Маргарита",
                "Михаил Афанасьевич Булгаков",
                "9785171123925",
                new BigDecimal("2021"),
                null,
                true,
                "Доставим бесплатно в среду, 28 октября",
                "https://s.f.kz/prod/1270/1269662_1000.jpg",
                "АСТ",
                2018,
                Binding.HARDCOVER));
    }

    @Test
    void shouldReadOldPriceOfDiscountedProduct() {
        ParsedOffer offer = product("product-295466-discount.html", "295466");

        assertThat(offer.price()).isEqualByComparingTo("1574");
        assertThat(offer.oldPrice()).isEqualByComparingTo("2249");
        assertThat(offer.isbn13()).isEqualTo("9785389016866");
        assertThat(offer.publisher()).isEqualTo("Азбука");
        assertThat(offer.year()).isEqualTo(2022);
        assertThat(offer.binding()).isEqualTo(Binding.HARDCOVER);
        assertThat(offer.inStock()).isTrue();
    }

    @Test
    void shouldTakeFirstValidIsbnWhenCellListsSeveral() {
        ParsedOffer offer = product("product-103811-several-isbn.html", "103811");

        assertThat(offer.isbn13()).isEqualTo("9785170569540");
        assertThat(offer.price()).isEqualByComparingTo("2674");
        assertThat(offer.year()).isEqualTo(2014);
        assertThat(offer.deliveryText()).isEqualTo("Доставим бесплатно 11 ноября");
    }

    @Test
    void shouldReadOutOfStockProduct() {
        ParsedOffer offer = product("product-out-of-stock-edited.html", "1269662");

        assertThat(offer.inStock()).isFalse();
        assertThat(offer.price()).isEqualByComparingTo("2021");
    }

    @Test
    void shouldReadProductWithoutIsbn() {
        ParsedOffer offer = product("product-no-isbn-edited.html", "1269662");

        assertThat(offer.isbn13()).isNull();
        assertThat(offer.author()).isEqualTo("Михаил Афанасьевич Булгаков");
        assertThat(offer.publisher()).isEqualTo("АСТ");
    }

    @Test
    void shouldReturnNothingForPageWithoutProduct() {
        Document home = Jsoup.parse("<html><body><h1>Flip.kz</h1></body></html>", "https://www.flip.kz/");

        assertThat(parser.parseProduct(home)).isEmpty();
    }

    @Test
    void shouldTakeFirstPublisherOfList() {
        assertThat(FlipParser.firstListItem("Эксмо, Магистраль. Главный тренд")).isEqualTo("Эксмо");
        assertThat(FlipParser.firstListItem("Clever")).isEqualTo("Clever");
    }

    private ParsedOffer product(String file, String id) {
        return parser.parseProduct(fixture(file, "https://www.flip.kz/catalog?prod=" + id)).orElseThrow();
    }

    private static Document fixture(String file, String url) {
        try (InputStream html = FlipParserTest.class.getResourceAsStream("/fixtures/flip/" + file)) {
            return Jsoup.parse(html, "UTF-8", url);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
