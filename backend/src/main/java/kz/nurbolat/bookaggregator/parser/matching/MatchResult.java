package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.offer.MatchStatus;

/**
 * @param edition matched edition, {@code null} unless {@code status} is MATCHED
 */
public record MatchResult(MatchStatus status, Edition edition) {

    static MatchResult matched(Edition edition) {
        return new MatchResult(MatchStatus.MATCHED, edition);
    }

    static MatchResult pendingReview() {
        return new MatchResult(MatchStatus.PENDING_REVIEW, null);
    }

    static MatchResult rejected() {
        return new MatchResult(MatchStatus.REJECTED, null);
    }
}
