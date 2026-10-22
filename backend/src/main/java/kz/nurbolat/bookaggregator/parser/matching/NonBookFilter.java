package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.common.text.TextNormalizer;

import java.util.regex.Pattern;

/**
 * Recognizes goods that stores list next to books (covers, bookmarks, postcards, planners) by words
 * in the title and by the store's category, so that they are rejected instead of becoming editions.
 */
final class NonBookFilter {

    private static final int FLAGS = Pattern.UNICODE_CHARACTER_CLASS;

    // Matched against normalized text: lower case, "е" instead of "ё", no punctuation
    private static final Pattern STOP_WORDS = Pattern.compile(
            "\\b(обложк[аи] для (книг|книги|учебник\\w*|паспорта)"
                    + "|закладк[аи]"
                    + "|набор открыток|открытк[аи]"
                    + "|ежедневник\\w*|планер\\w*|блокнот\\w*"
                    + "|стикер\\w*|подставк[аи] для книг"
                    + "|карт[аы] (складн|настенн)\\w*)\\b",
            FLAGS);
    private static final Pattern NON_BOOK_CATEGORIES = Pattern.compile(
            "\\b(канцеляр\\w*|канцтовар\\w*|сувенир\\w*|аксессуар\\w*|открытк\\w*|календар\\w*|игрушк\\w*)\\b",
            FLAGS);

    private NonBookFilter() {
    }

    /**
     * @param category the store's category of the product, {@code null} if unknown
     */
    static boolean isNonBook(String title, String category) {
        return matches(STOP_WORDS, title) || matches(NON_BOOK_CATEGORIES, category);
    }

    private static boolean matches(Pattern pattern, String text) {
        String normalized = TextNormalizer.normalize(text);
        return normalized != null && pattern.matcher(normalized).find();
    }
}
