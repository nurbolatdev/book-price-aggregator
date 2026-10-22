package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.book.CatalogService;
import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.metadata.BookMetadata;
import kz.nurbolat.bookaggregator.metadata.GoogleBooksClient;
import kz.nurbolat.bookaggregator.parser.core.ParsedOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Links a store offer to an edition (version 1, docs/05-parsing-policy.md, section 3):
 * by ISBN, creating the edition when it is new; without ISBN only on an exact match of the
 * normalized title and author, otherwise the offer waits for manual review.
 * Fuzzy matching follows in M16.
 */
@Component
@RequiredArgsConstructor
public class EditionMatcher {

    private final CatalogService catalogService;
    private final GoogleBooksClient googleBooksClient;

    public MatchResult match(ParsedOffer offer) {
        if (offer.isbn13() != null) {
            Edition edition = catalogService.findEditionByIsbn(offer.isbn13())
                    .orElseGet(() -> catalogService.findOrCreateEdition(metadataOf(offer)));
            return MatchResult.matched(edition);
        }
        if (offer.title() == null || offer.author() == null) {
            return MatchResult.pendingReview();
        }

        List<Edition> candidates = catalogService.findEditionsByExactTitleAndAuthor(offer.title(), offer.author());
        if (candidates.size() > 1) {
            candidates = candidates.stream().filter(edition -> sameEdition(edition, offer)).toList();
        }
        return candidates.size() == 1 ? MatchResult.matched(candidates.get(0)) : MatchResult.pendingReview();
    }

    /**
     * Google Books knows the work (title, author, description); the store page knows its own
     * printing (publisher, year, binding, cover). Each side fills what the other lacks.
     */
    private BookMetadata metadataOf(ParsedOffer offer) {
        BookMetadata google = googleBooksClient.findByIsbn(offer.isbn13()).orElse(null);
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

    // Among editions of the same work, publisher and year (when the store shows them) pick one
    private static boolean sameEdition(Edition edition, ParsedOffer offer) {
        boolean publisherMatches = offer.publisher() == null
                || Objects.equals(TextNormalizer.normalize(edition.getPublisher()), TextNormalizer.normalize(offer.publisher()));
        boolean yearMatches = offer.year() == null || edition.getYear() == null
                || edition.getYear().intValue() == offer.year();
        return publisherMatches && yearMatches;
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
