package kz.nurbolat.bookaggregator.parser.matching;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Trigram similarity computed the way pg_trgm's {@code similarity()} does it, so that decisions
 * made in Java agree with the candidates PostgreSQL returns: every word is padded with two spaces
 * in front and one behind, and the score is shared trigrams divided by all distinct trigrams.
 */
final class TrigramSimilarity {

    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");

    private TrigramSimilarity() {
    }

    /**
     * @return from 0 (nothing in common) to 1 (same trigrams); 0 when either text is {@code null} or has no words
     */
    static double similarity(String first, String second) {
        if (first == null || second == null) {
            return 0;
        }
        Set<String> a = trigrams(first);
        Set<String> b = trigrams(second);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int common = 0;
        for (String trigram : a) {
            if (b.contains(trigram)) {
                common++;
            }
        }
        return (double) common / (a.size() + b.size() - common);
    }

    private static Set<String> trigrams(String text) {
        Set<String> result = new HashSet<>();
        for (String word : NON_WORD.split(text.toLowerCase(Locale.ROOT))) {
            if (word.isEmpty()) {
                continue;
            }
            String padded = "  " + word + " ";
            for (int i = 0; i + 3 <= padded.length(); i++) {
                result.add(padded.substring(i, i + 3));
            }
        }
        return result;
    }
}
