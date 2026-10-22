package kz.nurbolat.bookaggregator.parser.meloman;

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
 * Parses saved meloman.kz pages (src/test/resources/fixtures/meloman); no network.
 */
class MelomanParserTest {

    private static final String FORD_URL = "https://www.meloman.kz/biographies/moja-zhizn-moi-dostizhenija.html";

    private final MelomanParser parser = new MelomanParser(mock(HtmlFetcher.class), new ObjectMapper());

    @Test
    void shouldReadAllProductsOfCatalogPage() {
        List<ParsedOffer> offers = parser.parseCatalog(fixture("listing-books-page-1.html", "https://www.meloman.kz/books/?p=1"), 1);

        assertThat(offers).hasSize(50);
        assertThat(offers.get(0)).isEqualTo(new ParsedOffer(
                "3272004",
                FORD_URL,
                "Моя жизнь. Мои достижения",
                "Форд Г.",
                null,
                new BigDecimal("4990"),
                null,
                true,
                null,
                "https://simg.marwin.kz/media/catalog/product/cache/caed27b441cb6475334305a4c65b6330/1/0/1012134292.jpg",
                null,
                null,
                null,
                "Книги/Публицистика. Биографии. Мемуары./Биографии. Мемуары"));
        assertThat(offers).extracting(ParsedOffer::title)
                .contains("Братья Карамазовы (твердый переплет)", "Книжка для купания. Книжка-проявляшка для малышей");
    }

    @Test
    void shouldReturnNothingPastLastCatalogPage() {
        // Magento answers a page number past the end with the last page; the pager tells which one it is
        Document lastPageShown = fixture("listing-books-page-1.html", "https://www.meloman.kz/books/?p=201");

        assertThat(parser.parseCatalog(lastPageShown, 201)).isEmpty();
    }

    @Test
    void shouldReadProductPageWithPriceFromPriceBox() {
        ParsedOffer offer = product("product-rouzvud-hardcover.html");

        // JSON-LD says 11 900 (loyalty-card price); a visitor pays 13 850
        assertThat(offer).isEqualTo(new ParsedOffer(
                "3579469",
                "https://www.meloman.kz/books/reading-memoirs-biographies/tru-krajm/rouzvud-dzh-bol-shaja-kniga-serijnyh-ubijc-150-biografij-man-jakov-so-vsego-mira.html",
                "Большая книга серийных убийц. 150 биографий маньяков со всего мира",
                "Роузвуд Дж.",
                "9785041667610",
                new BigDecimal("13850"),
                null,
                true,
                null,
                "https://simg.marwin.kz/media/catalog/product/b/o/bolshaya_kniga_seriynykh_ubiyts_150_biografiy_manyakov_so_vsego_mira.jpg",
                "БОМБОРА",
                2024,
                Binding.HARDCOVER,
                "Книги/Публицистика. Биографии. Мемуары./Тру-крайм"));
    }

    @Test
    void shouldReadProductWithoutIsbnFromProductJsonLd() {
        ParsedOffer offer = product("product-karamazovy-no-isbn.html");

        assertThat(offer.externalId()).isEqualTo("24014");
        assertThat(offer.title()).isEqualTo("Братья Карамазовы (твердый переплет)");
        assertThat(offer.author()).isEqualTo("Достоевский Ф. М.");
        assertThat(offer.isbn13()).isNull();
        assertThat(offer.price()).isEqualByComparingTo("2750");
        assertThat(offer.inStock()).isTrue();
        assertThat(offer.publisher()).isEqualTo("Азбука");
        assertThat(offer.year()).isEqualTo(2015);
        assertThat(offer.binding()).isEqualTo(Binding.HARDCOVER);
        assertThat(offer.coverUrl()).isEqualTo("https://simg.marwin.kz/media/catalog/product/1/0/1018075_0.jpg");
    }

    @Test
    void shouldReadOldPriceWhenProductIsDiscounted() {
        ParsedOffer offer = product("product-ford-discount-edited.html");

        assertThat(offer.price()).isEqualByComparingTo("4990");
        assertThat(offer.oldPrice()).isEqualByComparingTo("5990");
        assertThat(offer.isbn13()).isEqualTo("9785170779680");
        assertThat(offer.title()).isEqualTo("Моя жизнь. Мои достижения");
    }

    @Test
    void shouldMarkOutOfStockWhenStoreSaysSo() {
        assertThat(product("product-ford.html").inStock()).isTrue();
        assertThat(product("product-ford-out-of-stock-edited.html").inStock()).isFalse();
    }

    @Test
    void shouldReturnNothingForPageWithoutProduct() {
        Document home = Jsoup.parse("<html><body><h1>Meloman</h1></body></html>", "https://www.meloman.kz/");

        assertThat(parser.parseProduct(home)).isEmpty();
    }

    @Test
    void shouldSplitAuthorFromTitleOnlyWhenPrefixHasInitials() {
        assertThat(MelomanParser.authorAndTitle("Сент-Экзюпери А. де: Маленький принц"))
                .containsExactly("Сент-Экзюпери А. де", "Маленький принц");
        assertThat(MelomanParser.authorAndTitle("Nauryzbai G., Omarkulova S.: Salem, Аlmaty"))
                .containsExactly("Nauryzbai G., Omarkulova S.", "Salem, Аlmaty");
        assertThat(MelomanParser.authorAndTitle("Конец режима: Как закончились три европейские диктатуры"))
                .containsExactly(null, "Конец режима: Как закончились три европейские диктатуры");
    }

    @Test
    void shouldNeverSearchBecauseRobotsTxtForbidsIt() {
        assertThat(parser.canSearch()).isFalse();
        assertThatThrownBy(() -> parser.search("Мастер и Маргарита")).isInstanceOf(UnsupportedOperationException.class);
    }

    private ParsedOffer product(String file) {
        return parser.parseProduct(fixture(file, FORD_URL)).orElseThrow();
    }

    private static Document fixture(String file, String url) {
        try (InputStream in = MelomanParserTest.class.getResourceAsStream("/fixtures/meloman/" + file)) {
            return Jsoup.parse(in, "UTF-8", url);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
