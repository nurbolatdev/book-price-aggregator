package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.common.text.IsbnUtils;
import kz.nurbolat.bookaggregator.edition.Binding;
import org.jsoup.nodes.Element;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Values written the same way on every store page: tenge amounts, years, bindings, ISBN lists.
 */
public final class PageValues {

    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");

    private PageValues() {
    }

    /** Tenge amounts like "2 021 ₸" (with no-break spaces); {@code null} if there are no digits. */
    public static BigDecimal money(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("\\D", "");
        return digits.isEmpty() ? null : new BigDecimal(digits);
    }

    /** First year in the text ("твердый переплет, 2023"), {@code null} if none. */
    public static Integer year(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    /** "Твердый переплет", "мягкая обложка"; {@link Binding#UNKNOWN} otherwise. */
    public static Binding binding(String text) {
        if (text == null) {
            return Binding.UNKNOWN;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("тверд") || lower.contains("твёрд")) {
            return Binding.HARDCOVER;
        }
        if (lower.contains("мягк")) {
            return Binding.PAPERBACK;
        }
        return Binding.UNKNOWN;
    }

    /** "978-5-17-056954-0, 978-5-271-27312-4": the first valid ISBN as ISBN-13, {@code null} if none. */
    public static String firstIsbn(String text) {
        if (text == null) {
            return null;
        }
        for (String candidate : text.split("[,;]")) {
            String isbn13 = IsbnUtils.normalize(candidate);
            if (isbn13 != null) {
                return isbn13;
            }
        }
        return null;
    }

    /** Element text with no-break spaces turned into plain ones; {@code null} when missing or blank. */
    public static String text(Element root, String selector) {
        Element element = root.selectFirst(selector);
        return element == null ? null : clean(element.text());
    }

    /** Text with no-break and thin spaces turned into plain ones and collapsed; {@code null} when blank. */
    public static String clean(String text) {
        if (text == null) {
            return null;
        }
        String result = text.replace(' ', ' ').replace(' ', ' ').replace(' ', ' ')
                .replaceAll("\\s+", " ").strip();
        return result.isEmpty() ? null : result;
    }
}
