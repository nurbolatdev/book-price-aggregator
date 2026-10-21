package kz.nurbolat.bookaggregator.common.text;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizes titles and author names so that differently written variants
 * ("Мастер и Маргарита", «МАСТЕР и маргарита!») compare equal in search and matching.
 */
public final class TextNormalizer {

    private static final Pattern QUOTES = Pattern.compile("[«»\"'„“”‘’`]");
    private static final Pattern PUNCTUATION = Pattern.compile("[\\p{P}\\p{S}]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextNormalizer() {
    }

    /**
     * Lower-cases, replaces "ё" with "е", drops quotes, turns other punctuation into spaces
     * and collapses whitespace. Returns {@code null} for {@code null}.
     */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String result = text.toLowerCase(Locale.ROOT).replace('ё', 'е');
        result = QUOTES.matcher(result).replaceAll("");
        result = PUNCTUATION.matcher(result).replaceAll(" ");
        return SPACES.matcher(result).replaceAll(" ").strip();
    }
}
