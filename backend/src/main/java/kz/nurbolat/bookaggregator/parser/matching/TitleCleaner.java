package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.edition.Binding;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a store's product title into a book title: drops what stores add around it (binding,
 * "подарочное издание", series, illustrations, "Комплект из 2 книг", a leading "Книга", article numbers,
 * quotes) and keeps the binding and year it mentioned, since those describe the edition.
 */
final class TitleCleaner {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    // (твёрдая обложка), [подарочное издание], (эксклюзивная классика), (2021), (арт. 12345)
    private static final Pattern BRACKETS = Pattern.compile("[(\\[]([^()\\[\\]]*)[)\\]]");
    private static final Pattern JUNK_IN_BRACKETS = Pattern.compile(
            "обложк|переплет|переплёт|подарочн|издани|классик|\\bсерия\\b|коллекци|pocket|покет|иллюстрац"
                    + "|комплект|\\bтом|кітап|жина|тілінде|\\bязык"
                    + "|\\bарт\\b|артикул|^\\s*\\d{4}\\s*(г\\.?|год)?\\s*$|^\\s*\\d{5,}\\s*$",
            FLAGS);
    private static final Pattern BINDING_PHRASE = Pattern.compile(
            "\\b(тв[её]рд|мягк)\\w*\\s+(обложк|переплет|переплёт)\\w*", FLAGS);
    private static final Pattern GIFT_EDITION = Pattern.compile("\\bподарочное\\s+издание\\b", FLAGS);
    private static final Pattern SERIES = Pattern.compile("\\bэксклюзивная\\s+классика\\b", FLAGS);
    // "Маленький принц. С иллюстрациями автора", "Мастер и Маргарита с иллюстрациями Нади Рушевой"
    private static final Pattern ILLUSTRATED = Pattern.compile("[.,]?\\s+с\\s+(\\w+\\s+)?иллюстрациями\\b.*$", FLAGS);
    // "Война и мир. Комплект из 2 книг", "Шантарам — 1. Комплект из 2 книг"
    private static final Pattern SET = Pattern.compile(
            "(\\s*[–—-]\\s*\\d+)?[.,]?\\s*комплект\\s+из\\s+\\d+(-х)?\\s+(книг|томов|частей)\\b.*$", FLAGS);
    private static final Pattern ARTICLE = Pattern.compile("\\b(арт\\.?|артикул)\\s*[:№#]?\\s*[\\w-]*\\d[\\w-]*", FLAGS);
    // "1984" alone is a title, so a year counts only with "г." or "год" after it
    private static final Pattern YEAR_PHRASE = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\s*(г\\.|года?\\b)", FLAGS);
    // "Книга: Дюна", "Книга «Дюна»", but not "Книга джунглей"
    private static final Pattern LEADING_BOOK = Pattern.compile("^\\s*книга\\s*(?:[:–—-]\\s*|(?=[«\"„“]))", FLAGS);
    private static final Pattern DOUBLE_QUOTES = Pattern.compile("[«»\"„“”]");
    private static final Pattern YEAR = Pattern.compile("\\b(1[5-9]\\d{2}|20\\d{2})\\b");
    private static final Pattern SEPARATOR_RUN = Pattern.compile("(\\s*[,;:–—-]){2,}\\s*");
    private static final Pattern EDGES = Pattern.compile("^[\\s,;:.–—-]+|[\\s,;:.–—-]+$");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TitleCleaner() {
    }

    /**
     * @param binding binding named in the title, {@code null} if none
     * @param year    publication year named in the title, {@code null} if none
     */
    record CleanTitle(String title, Binding binding, Integer year) {
    }

    /**
     * @return the cleaned title; the original (stripped) one if cleaning would leave nothing, {@code null} for {@code null}
     */
    static CleanTitle clean(String raw) {
        if (raw == null) {
            return new CleanTitle(null, null, null);
        }
        Binding binding = binding(raw);
        Integer year = null;

        StringBuilder kept = new StringBuilder();
        Matcher brackets = BRACKETS.matcher(raw);
        while (brackets.find()) {
            String inside = brackets.group(1);
            boolean junk = inside.isBlank() || JUNK_IN_BRACKETS.matcher(inside).find();
            if (junk && year == null) {
                year = year(inside);
            }
            brackets.appendReplacement(kept, junk ? " " : Matcher.quoteReplacement(brackets.group()));
        }
        brackets.appendTail(kept);

        String title = kept.toString();
        Matcher yearPhrase = YEAR_PHRASE.matcher(title);
        if (yearPhrase.find() && year == null) {
            year = Integer.valueOf(yearPhrase.group(1));
        }
        title = YEAR_PHRASE.matcher(title).replaceAll(" ");
        title = BINDING_PHRASE.matcher(title).replaceAll(" ");
        title = GIFT_EDITION.matcher(title).replaceAll(" ");
        title = SERIES.matcher(title).replaceAll(" ");
        title = ILLUSTRATED.matcher(title).replaceFirst("");
        title = SET.matcher(title).replaceFirst("");
        title = ARTICLE.matcher(title).replaceAll(" ");
        title = LEADING_BOOK.matcher(title).replaceFirst("");
        title = DOUBLE_QUOTES.matcher(title).replaceAll("");
        title = SEPARATOR_RUN.matcher(title).replaceAll(", ");
        title = SPACES.matcher(title).replaceAll(" ");
        title = EDGES.matcher(title).replaceAll("");

        return new CleanTitle(title.isEmpty() ? raw.strip() : title, binding, year);
    }

    private static Binding binding(String text) {
        Matcher matcher = BINDING_PHRASE.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).toLowerCase(Locale.ROOT).startsWith("мягк") ? Binding.PAPERBACK : Binding.HARDCOVER;
    }

    private static Integer year(String text) {
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
