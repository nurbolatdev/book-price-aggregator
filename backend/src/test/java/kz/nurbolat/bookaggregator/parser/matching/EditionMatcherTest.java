package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditionMatcherTest {

    private static final String ISBN = "9780306406157";

    private final CatalogService catalog = mock(CatalogService.class);
    private final GoogleBooksClient google = mock(GoogleBooksClient.class);
    private final EditionMatcher matcher = new EditionMatcher(catalog, google);

    @Test
    void shouldMatchExistingEditionByIsbn() {
        Edition edition = edition(1L, "АСТ", 2018);
        when(catalog.findEditionByIsbn(ISBN)).thenReturn(Optional.of(edition));

        MatchResult result = matcher.match(offer(ISBN, "Мастер и Маргарита", "Михаил Булгаков", "АСТ", 2018));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, edition));
        verify(google, never()).findByIsbn(any());
    }

    @Test
    void shouldCreateEditionFromGoogleAndStoreDataWhenIsbnIsNew() {
        Edition created = edition(2L, "Азбука", 2022);
        when(catalog.findEditionByIsbn(ISBN)).thenReturn(Optional.empty());
        when(google.findByIsbn(ISBN)).thenReturn(Optional.of(new BookMetadata("Мастер и Маргарита",
                "Михаил Булгаков", "Роман", ISBN, "Google Publisher", 2019, 480, "ru", "https://google/cover", null)));
        when(catalog.findOrCreateEdition(any())).thenReturn(created);

        MatchResult result = matcher.match(offer(ISBN, "МАСТЕР И МАРГАРИТА (подарочное)", "Булгаков М.", "Азбука", 2022));

        assertThat(result.status()).isEqualTo(MatchStatus.MATCHED);
        ArgumentCaptor<BookMetadata> metadata = ArgumentCaptor.forClass(BookMetadata.class);
        verify(catalog).findOrCreateEdition(metadata.capture());
        // the work from Google, the printing from the store page
        assertThat(metadata.getValue().title()).isEqualTo("Мастер и Маргарита");
        assertThat(metadata.getValue().author()).isEqualTo("Михаил Булгаков");
        assertThat(metadata.getValue().publisher()).isEqualTo("Азбука");
        assertThat(metadata.getValue().year()).isEqualTo(2022);
        assertThat(metadata.getValue().pages()).isEqualTo(480);
        assertThat(metadata.getValue().binding()).isEqualTo(Binding.HARDCOVER);
    }

    @Test
    void shouldMatchByExactTitleAndAuthorWhenThereIsNoIsbn() {
        Edition only = edition(3L, "АСТ", 2018);
        when(catalog.findEditionsByExactTitleAndAuthor("«Мастер и Маргарита»", "Михаил Булгаков"))
                .thenReturn(List.of(only));

        MatchResult result = matcher.match(offer(null, "«Мастер и Маргарита»", "Михаил Булгаков", null, null));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, only));
    }

    @Test
    void shouldPickEditionByPublisherAndYearWhenWorkHasSeveral() {
        Edition ast = edition(4L, "АСТ", 2018);
        Edition azbuka = edition(5L, "Азбука", 2022);
        when(catalog.findEditionsByExactTitleAndAuthor(any(), any())).thenReturn(List.of(ast, azbuka));

        MatchResult result = matcher.match(offer(null, "Мастер и Маргарита", "Михаил Булгаков", "азбука", 2022));

        assertThat(result.edition()).isEqualTo(azbuka);
    }

    @Test
    void shouldLeaveForReviewWhenNoExactMatchOrNoAuthor() {
        when(catalog.findEditionsByExactTitleAndAuthor(any(), any())).thenReturn(List.of());

        assertThat(matcher.match(offer(null, "Мастер", "Булгаков", null, null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
        assertThat(matcher.match(offer(null, "Мастер и Маргарита", null, null, null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
    }

    @Test
    void shouldLeaveForReviewWhenSeveralEditionsStillFit() {
        when(catalog.findEditionsByExactTitleAndAuthor(any(), any()))
                .thenReturn(List.of(edition(6L, "АСТ", 2018), edition(7L, "АСТ", 2020)));

        assertThat(matcher.match(offer(null, "Мастер и Маргарита", "Михаил Булгаков", "АСТ", null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
    }

    private static ParsedOffer offer(String isbn, String title, String author, String publisher, Integer year) {
        return new ParsedOffer("1", "https://www.flip.kz/catalog?prod=1", title, author, isbn,
                new BigDecimal("2000"), null, true, null, null, publisher, year, Binding.HARDCOVER);
    }

    private static Edition edition(Long id, String publisher, int year) {
        Edition edition = new Edition();
        edition.setId(id);
        edition.setPublisher(publisher);
        edition.setYear((short) year);
        return edition;
    }
}
