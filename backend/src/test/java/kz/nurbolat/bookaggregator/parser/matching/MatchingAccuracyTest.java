package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How often the matcher decides right on real store titles (src/test/resources/matching/cases.csv)
 * against the demo catalog (catalog.csv). Candidates are chosen as PostgreSQL does: books whose
 * title is at least 0.3 similar (the default threshold of pg_trgm's {@code %}), the 5 most similar.
 */
class MatchingAccuracyTest {

    private static final double REQUIRED_ACCURACY = 0.85;
    private static final double PG_TRGM_THRESHOLD = 0.3;
    private static final String OTHER = "OTHER";
    private static final String REJECTED = "REJECTED";

    private final MatchingProperties properties = new MatchingProperties(0.85, 0.7, 0.6, 5);

    @Test
    void shouldDecideRightForAtLeast85PercentOfRealStoreTitles() {
        List<Edition> catalog = readCatalog();
        Map<Long, Edition> byId = catalog.stream().collect(Collectors.toMap(Edition::getId, Function.identity()));
        Edition created = new Edition();
        CatalogService catalogService = mock(CatalogService.class);
        EditionCandidateRepository candidates = mock(EditionCandidateRepository.class);
        when(candidates.findEditionIdsOfSimilarBooks(any(), anyInt())).thenAnswer(call -> {
            String title = call.getArgument(0);
            return catalog.stream()
                    .filter(edition -> TrigramSimilarity.similarity(edition.getBook().getTitleNorm(), title) >= PG_TRGM_THRESHOLD)
                    .sorted(Comparator.comparingDouble(
                            (Edition edition) -> TrigramSimilarity.similarity(edition.getBook().getTitleNorm(), title)).reversed())
                    .limit(call.<Integer>getArgument(1))
                    .map(Edition::getId)
                    .toList();
        });
        when(catalogService.findEditionsWithBook(anyList()))
                .thenAnswer(call -> call.<List<Long>>getArgument(0).stream().map(byId::get).toList());
        when(catalogService.findOrCreateEdition(any())).thenReturn(created);
        EditionMatcher matcher = new EditionMatcher(catalogService, mock(GoogleBooksClient.class), candidates, properties);

        List<String[]> cases = readCsv("/matching/cases.csv");
        List<String> wrong = new ArrayList<>();
        for (String[] row : cases) {
            ParsedOffer offer = new ParsedOffer("1", "https://store/1", row[1], row[2].isBlank() ? null : row[2], null,
                    BigDecimal.ONE, null, true, null, null, null, null, Binding.UNKNOWN);
            MatchResult result = matcher.match(offer);
            String decided = switch (result.status()) {
                case REJECTED -> REJECTED;
                case PENDING_REVIEW -> OTHER;
                case MATCHED -> result.edition() == created ? OTHER : result.edition().getBook().getTitle();
            };
            if (!decided.equals(row[3])) {
                wrong.add("%s «%s» / %s: expected %s, got %s (%s)".formatted(row[0], row[1], row[2], row[3], decided,
                        result.status()));
            }
        }

        double accuracy = 1.0 - (double) wrong.size() / cases.size();
        System.out.printf("Matching accuracy: %d of %d right (%.1f%%)%n", cases.size() - wrong.size(), cases.size(),
                accuracy * 100);
        wrong.forEach(line -> System.out.println("  wrong: " + line));
        assertThat(cases).hasSizeGreaterThanOrEqualTo(30);
        assertThat(accuracy).as("wrong decisions: %s", wrong).isGreaterThanOrEqualTo(REQUIRED_ACCURACY);
    }

    private static List<Edition> readCatalog() {
        List<Edition> editions = new ArrayList<>();
        long id = 1;
        for (String[] row : readCsv("/matching/catalog.csv")) {
            Book book = new Book();
            book.setTitle(row[0]);
            book.setAuthor(row[1]);
            book.setTitleNorm(TextNormalizer.normalize(row[0]));
            book.setAuthorNorm(TextNormalizer.normalize(row[1]));
            Edition edition = new Edition();
            edition.setId(id++);
            edition.setBook(book);
            editions.add(edition);
        }
        return editions;
    }

    // ';'-separated, '#' comments, the first other line is the header
    private static List<String[]> readCsv(String resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                MatchingAccuracyTest.class.getResourceAsStream(resource), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .skip(1)
                    .map(line -> line.split(";", -1))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
