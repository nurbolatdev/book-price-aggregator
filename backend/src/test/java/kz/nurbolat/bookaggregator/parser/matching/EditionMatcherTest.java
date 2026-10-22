package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EditionMatcherTest {

    private static final String ISBN = "9780306406157";

    private final CatalogService catalog = mock(CatalogService.class);
    private final GoogleBooksClient google = mock(GoogleBooksClient.class);
    private final EditionCandidateRepository candidates = mock(EditionCandidateRepository.class);
    private final EditionMatcher matcher = new EditionMatcher(catalog, google, candidates,
            new MatchingProperties(0.85, 0.7, 0.6, 5));

    @Test
    void shouldMatchExistingEditionByIsbn() {
        Edition edition = edition(1L, book("Мастер и Маргарита", "Михаил Булгаков"), "АСТ", 2018);
        when(catalog.findEditionByIsbn(ISBN)).thenReturn(Optional.of(edition));

        MatchResult result = matcher.match(offer(ISBN, "Мастер и Маргарита", "Михаил Булгаков", "АСТ", 2018));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, edition));
        verify(google, never()).findByIsbn(any());
    }

    @Test
    void shouldCreateEditionFromGoogleAndStoreDataWhenIsbnIsNew() {
        Edition created = edition(2L, book("Мастер и Маргарита", "Михаил Булгаков"), "Азбука", 2022);
        when(catalog.findEditionByIsbn(ISBN)).thenReturn(Optional.empty());
        when(google.findByIsbn(ISBN)).thenReturn(Optional.of(new BookMetadata("Мастер и Маргарита",
                "Михаил Булгаков", "Роман", ISBN, "Google Publisher", 2019, 480, "ru", "https://google/cover", null)));
        when(catalog.findOrCreateEdition(any())).thenReturn(created);

        MatchResult result = matcher.match(offer(ISBN, "МАСТЕР И МАРГАРИТА (подарочное)", "Булгаков М.", "Азбука", 2022));

        assertThat(result.status()).isEqualTo(MatchStatus.MATCHED);
        BookMetadata metadata = createdMetadata();
        // the work from Google, the printing from the store page
        assertThat(metadata.title()).isEqualTo("Мастер и Маргарита");
        assertThat(metadata.author()).isEqualTo("Михаил Булгаков");
        assertThat(metadata.publisher()).isEqualTo("Азбука");
        assertThat(metadata.year()).isEqualTo(2022);
        assertThat(metadata.pages()).isEqualTo(480);
        assertThat(metadata.binding()).isEqualTo(Binding.HARDCOVER);
    }

    @Test
    void shouldAddNewIsbnAsEditionOfSimilarBookWhenStoreWritesAuthorDifferently() {
        Book work = book("Самый богатый человек в Вавилоне", "Джордж Клейсон");
        Edition known = edition(12L, work, "Эксмо", 2021);
        Edition added = edition(13L, work, "МИФ", 2024);
        when(catalog.findEditionByIsbn(ISBN)).thenReturn(Optional.empty());
        candidatesAre(known);
        when(catalog.addEdition(any(), any())).thenReturn(added);

        MatchResult result = matcher.match(offer(ISBN, "Самый богатый человек в Вавилоне", "Клейсон Дж.", "МИФ", 2024));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, added));
        verify(catalog).addEdition(eq(work), any());
        verify(catalog, never()).findOrCreateEdition(any());
    }

    @Test
    void shouldMatchEditionWithoutPublisherWhenStoreNamesOne() {
        Edition only = edition(14L, book("Спеши любить", "Николас Спаркс"), null, 2020);
        candidatesAre(only);

        assertThat(matcher.match(offer(null, "Спеши любить (эксклюзивная классика)", "Спаркс Николас", "АСТ", null)))
                .isEqualTo(new MatchResult(MatchStatus.MATCHED, only));
    }

    @Test
    void shouldMatchSimilarTitleAndAuthorWhenThereIsNoIsbn() {
        Edition only = edition(3L, book("Мастер и Маргарита", "Михаил Булгаков"), "АСТ", 2018);
        candidatesAre(only);

        MatchResult result = matcher.match(
                offer(null, "Книга «Мастер и Маргарита» (твёрдая обложка)", "Булгаков М. А.", null, null));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, only));
        verify(candidates).findEditionIdsOfSimilarBooks("мастер и маргарита", 5);
    }

    @Test
    void shouldPickEditionByPublisherAndYearWhenWorkHasSeveral() {
        Book work = book("Мастер и Маргарита", "Михаил Булгаков");
        Edition ast = edition(4L, work, "АСТ", 2018);
        Edition azbuka = edition(5L, work, "Азбука", 2022);
        candidatesAre(ast, azbuka);

        MatchResult result = matcher.match(offer(null, "Мастер и Маргарита", "Михаил Булгаков", "азбука", 2022));

        assertThat(result.edition()).isEqualTo(azbuka);
    }

    @Test
    void shouldLeaveForReviewWhenSeveralEditionsStillFit() {
        Book work = book("Мастер и Маргарита", "Михаил Булгаков");
        candidatesAre(edition(6L, work, "АСТ", 2018), edition(7L, work, "АСТ", 2020));

        assertThat(matcher.match(offer(null, "Мастер и Маргарита", "Михаил Булгаков", "АСТ", null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
    }

    @Test
    void shouldLeaveForReviewWhenTitleIsOnlySimilar() {
        candidatesAre(edition(8L, book("Пикник на обочине", "Аркадий Стругацкий, Борис Стругацкий"), "АСТ", 2020));

        // similarity 0.75: between the review and the match thresholds
        MatchResult result = matcher.match(offer(null, "Пикник на обочине роман", "Стругацкие А. и Б.", null, null));

        assertThat(result.status()).isEqualTo(MatchStatus.PENDING_REVIEW);
        verify(catalog, never()).findOrCreateEdition(any());
    }

    @Test
    void shouldLeaveForReviewWhenSameTitleHasAnotherAuthor() {
        candidatesAre(edition(9L, book("Идиот", "Федор Достоевский"), "АСТ", 2020));

        assertThat(matcher.match(offer(null, "Идиот", "Иван Петров", null, null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
    }

    @Test
    void shouldCreateBookWhenNoTitleIsSimilarAndAuthorIsKnown() {
        Edition created = edition(10L, book("Трудно быть богом", "Аркадий Стругацкий"), "АСТ", 2021);
        candidatesAre(edition(11L, book("Пикник на обочине", "Аркадий Стругацкий"), "АСТ", 2020));
        when(catalog.findOrCreateEdition(any())).thenReturn(created);

        MatchResult result = matcher.match(
                offer(null, "Трудно быть богом (мягкая обложка, 2021)", "Аркадий Стругацкий", "АСТ", null));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.MATCHED, created));
        BookMetadata metadata = createdMetadata();
        assertThat(metadata.title()).isEqualTo("Трудно быть богом");
        assertThat(metadata.year()).isEqualTo(2021);
        assertThat(metadata.binding()).isEqualTo(Binding.HARDCOVER);
        verifyNoInteractions(google);
    }

    @Test
    void shouldLeaveForReviewWhenNothingIsSimilarAndAuthorIsUnknown() {
        candidatesAre();

        assertThat(matcher.match(offer(null, "Трудно быть богом", null, null, null)).status())
                .isEqualTo(MatchStatus.PENDING_REVIEW);
        verify(catalog, never()).findOrCreateEdition(any());
    }

    @Test
    void shouldRejectGoodsThatAreNotBooks() {
        MatchResult result = matcher.match(offer(ISBN, "Закладка для книг «Мастер и Маргарита»", null, null, null));

        assertThat(result).isEqualTo(new MatchResult(MatchStatus.REJECTED, null));
        verifyNoInteractions(catalog, google, candidates);
    }

    @Test
    void shouldCompareAuthorsByNamesWhenStoreWritesThemDifferently() {
        assertThat(EditionMatcher.authorSimilarity("Булгаков М. А.", "михаил булгаков")).isEqualTo(1.0);
        assertThat(EditionMatcher.authorSimilarity("Булгаков Михаил Афанасьевич", "михаил булгаков")).isEqualTo(1.0);
        assertThat(EditionMatcher.authorSimilarity("Михаил Шолохов", "михаил булгаков")).isLessThan(0.7);
        assertThat(EditionMatcher.authorSimilarity("Стивен Хокинг", "стивен кинг")).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(EditionMatcher.authorSimilarity(null, "михаил булгаков")).isZero();
    }

    private void candidatesAre(Edition... editions) {
        List<Long> ids = Arrays.stream(editions).map(Edition::getId).toList();
        when(candidates.findEditionIdsOfSimilarBooks(any(), anyInt())).thenReturn(ids);
        when(catalog.findEditionsWithBook(anyList())).thenReturn(List.of(editions));
    }

    private BookMetadata createdMetadata() {
        ArgumentCaptor<BookMetadata> metadata = ArgumentCaptor.forClass(BookMetadata.class);
        verify(catalog).findOrCreateEdition(metadata.capture());
        return metadata.getValue();
    }

    private static ParsedOffer offer(String isbn, String title, String author, String publisher, Integer year) {
        return new ParsedOffer("1", "https://www.flip.kz/catalog?prod=1", title, author, isbn,
                new BigDecimal("2000"), null, true, null, null, publisher, year, Binding.HARDCOVER);
    }

    private static Book book(String title, String author) {
        Book book = new Book();
        book.setTitle(title);
        book.setAuthor(author);
        book.setTitleNorm(TextNormalizer.normalize(title));
        book.setAuthorNorm(TextNormalizer.normalize(author));
        return book;
    }

    private static Edition edition(Long id, Book book, String publisher, Integer year) {
        Edition edition = new Edition();
        edition.setId(id);
        edition.setBook(book);
        edition.setPublisher(publisher);
        edition.setYear(year == null ? null : year.shortValue());
        return edition;
    }
}
