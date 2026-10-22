package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import kz.nurbolat.bookaggregator.parser.matching.TitleCleaner.CleanTitle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Links a store offer to an edition (docs/05-parsing-policy.md, section 3):
 * <ol>
 *     <li>goods that are not books are rejected;</li>
 *     <li>by ISBN; a new ISBN becomes a new edition of the book it clearly belongs to, or a new book;</li>
 *     <li>without ISBN, by the cleaned title among the books most similar to it (pg_trgm): title and author
 *     similar enough and the same publisher and year, if the store shows them, give a match; a similar title
 *     alone waits for manual review; a title unlike any book becomes a new book when the author is known.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class EditionMatcher {

    // Initials ("м а" from "Булгаков М. А.") say nothing about who the author is
    private static final int MIN_NAME_LENGTH = 3;

    private final CatalogService catalogService;
    private final GoogleBooksClient googleBooksClient;
    private final EditionCandidateRepository candidateRepository;
    private final MatchingProperties properties;

    public MatchResult match(ParsedOffer storeOffer) {
        if (NonBookFilter.isNonBook(storeOffer.title(), storeOffer.category())) {
            return MatchResult.rejected();
        }
        ParsedOffer offer = withCleanTitle(storeOffer);
        if (offer.isbn13() != null) {
            Edition edition = catalogService.findEditionByIsbn(offer.isbn13())
                    .orElseGet(() -> newEdition(offer));
            return MatchResult.matched(edition);
        }
        if (offer.title() == null) {
            return MatchResult.pendingReview();
        }

        Candidates candidates = candidates(offer);
        List<Edition> confident = candidates.confident();
        if (!confident.isEmpty()) {
            long books = confident.stream().map(Edition::getBook).distinct().count();
            List<Edition> fitting = confident.stream().filter(edition -> sameEdition(edition, offer)).toList();
            return books == 1 && fitting.size() == 1 ? MatchResult.matched(fitting.get(0)) : MatchResult.pendingReview();
        }
        if (candidates.bestTitle() >= properties.reviewThreshold() || offer.author() == null) {
            return MatchResult.pendingReview();
        }
        return MatchResult.matched(catalogService.findOrCreateEdition(metadataOf(offer)));
    }

    /**
     * A printing not in the catalog yet: an edition of the book it is a printing of, when one book is clearly
     * that work (stores write authors differently, so the exact lookup of the catalog would add a second book).
     */
    private Edition newEdition(ParsedOffer offer) {
        BookMetadata metadata = metadataOf(offer);
        if (offer.title() == null) {
            return catalogService.findOrCreateEdition(metadata);
        }
        List<Book> works = candidates(offer).confident().stream().map(Edition::getBook).distinct().toList();
        return works.size() == 1
                ? catalogService.addEdition(works.get(0), metadata)
                : catalogService.findOrCreateEdition(metadata);
    }

    /**
     * Editions of the books most similar by title; confident ones also have a similar author.
     */
    private Candidates candidates(ParsedOffer offer) {
        String titleNorm = TextNormalizer.normalize(offer.title());
        List<Edition> editions = catalogService.findEditionsWithBook(
                candidateRepository.findEditionIdsOfSimilarBooks(titleNorm, properties.candidates()));
        double bestTitle = 0;
        List<Edition> confident = new ArrayList<>();
        for (Edition candidate : editions) {
            double title = TrigramSimilarity.similarity(titleNorm, candidate.getBook().getTitleNorm());
            double author = authorSimilarity(offer.author(), candidate.getBook().getAuthorNorm());
            bestTitle = Math.max(bestTitle, title);
            if (title >= properties.titleThreshold() && author >= properties.authorThreshold()) {
                confident.add(candidate);
            }
        }
        return new Candidates(confident, bestTitle);
    }

    private record Candidates(List<Edition> confident, double bestTitle) {
    }

    /**
     * Similarity of the whole names, or of the names word by word when that is higher: stores write
     * "Булгаков М. А." or "Булгаков Михаил Афанасьевич" for the catalog's "Михаил Булгаков". Each name of the
     * shorter list is compared with its closest name in the other one, ignoring initials.
     */
    static double authorSimilarity(String offerAuthor, String bookAuthorNorm) {
        String author = TextNormalizer.normalize(offerAuthor);
        if (author == null || author.isEmpty() || bookAuthorNorm == null) {
            return 0;
        }
        double whole = TrigramSimilarity.similarity(author, bookAuthorNorm);
        List<String> offerNames = names(author);
        List<String> bookNames = names(bookAuthorNorm);
        if (offerNames.isEmpty() || bookNames.isEmpty()) {
            return whole;
        }
        List<String> shorter = offerNames.size() <= bookNames.size() ? offerNames : bookNames;
        List<String> longer = shorter == offerNames ? bookNames : offerNames;
        double sum = 0;
        for (String name : shorter) {
            sum += longer.stream().mapToDouble(other -> TrigramSimilarity.similarity(name, other)).max().orElse(0);
        }
        return Math.max(whole, sum / shorter.size());
    }

    private static List<String> names(String normalizedAuthor) {
        return Arrays.stream(normalizedAuthor.split(" ")).filter(name -> name.length() >= MIN_NAME_LENGTH).toList();
    }

    // The cleaned title replaces the store's one; binding and year found in it fill what the page lacked
    private static ParsedOffer withCleanTitle(ParsedOffer offer) {
        CleanTitle clean = TitleCleaner.clean(offer.title());
        Binding binding = (offer.binding() == null || offer.binding() == Binding.UNKNOWN) && clean.binding() != null
                ? clean.binding() : offer.binding();
        Integer year = offer.year() != null ? offer.year() : clean.year();
        return new ParsedOffer(offer.externalId(), offer.url(), clean.title(), offer.author(), offer.isbn13(),
                offer.price(), offer.oldPrice(), offer.inStock(), offer.deliveryText(), offer.coverUrl(),
                offer.publisher(), year, binding, offer.category());
    }

    /**
     * Google Books knows the work (title, author, description); the store page knows its own
     * printing (publisher, year, binding, cover). Each side fills what the other lacks.
     */
    private BookMetadata metadataOf(ParsedOffer offer) {
        BookMetadata google = offer.isbn13() == null ? null : googleBooksClient.findByIsbn(offer.isbn13()).orElse(null);
        if (google == null) {
            return new BookMetadata(offer.title(), offer.author(), null, offer.isbn13(), offer.publisher(),
                    offer.year(), null, null, offer.coverUrl(), offer.binding());
        }
        return new BookMetadata(
                firstNonNull(google.title(), offer.title()),
                firstNonNull(google.author(), offer.author()),
                google.description(),
                offer.isbn13(),
                firstNonNull(offer.publisher(), google.publisher()),
                firstNonNull(offer.year(), google.year()),
                google.pages(),
                google.language(),
                firstNonNull(offer.coverUrl(), google.coverUrl()),
                offer.binding());
    }

    // Among editions of the same work, publisher and year (when both sides know them) pick one
    private static boolean sameEdition(Edition edition, ParsedOffer offer) {
        boolean publisherMatches = offer.publisher() == null || edition.getPublisher() == null
                || Objects.equals(TextNormalizer.normalize(edition.getPublisher()), TextNormalizer.normalize(offer.publisher()));
        boolean yearMatches = offer.year() == null || edition.getYear() == null
                || edition.getYear().intValue() == offer.year();
        return publisherMatches && yearMatches;
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
