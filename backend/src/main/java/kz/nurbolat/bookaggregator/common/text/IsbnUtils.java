package kz.nurbolat.bookaggregator.common.text;

/**
 * ISBN parsing: every valid ISBN is reduced to its 13-digit form, the natural key of an edition.
 */
public final class IsbnUtils {

    private IsbnUtils() {
    }

    /**
     * Strips separators, converts ISBN-10 to ISBN-13 and validates the check digit.
     *
     * @return 13 digits, or {@code null} if the input is not a valid ISBN
     */
    public static String normalize(String isbn) {
        if (isbn == null) {
            return null;
        }
        String compact = isbn.replaceAll("[\\s-]", "").toUpperCase();
        if (compact.matches("\\d{13}")) {
            return isValidIsbn13(compact) ? compact : null;
        }
        if (compact.matches("\\d{9}[\\dX]")) {
            return isValidIsbn10(compact) ? toIsbn13(compact) : null;
        }
        return null;
    }

    private static boolean isValidIsbn13(String digits) {
        boolean bookland = digits.startsWith("978") || digits.startsWith("979");
        return bookland && isbn13CheckDigit(digits.substring(0, 12)) == digits.charAt(12) - '0';
    }

    private static boolean isValidIsbn10(String isbn10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn10.charAt(i);
            int value = c == 'X' ? 10 : c - '0';
            sum += (10 - i) * value;
        }
        return sum % 11 == 0;
    }

    private static String toIsbn13(String isbn10) {
        String base = "978" + isbn10.substring(0, 9);
        return base + isbn13CheckDigit(base);
    }

    private static int isbn13CheckDigit(String first12) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12.charAt(i) - '0';
            sum += i % 2 == 0 ? digit : digit * 3;
        }
        return (10 - sum % 10) % 10;
    }
}
