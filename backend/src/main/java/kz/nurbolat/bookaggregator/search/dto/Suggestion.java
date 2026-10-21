package kz.nurbolat.bookaggregator.search.dto;

/**
 * Autocomplete entry.
 *
 * @param label "Название — Автор", ready to show
 */
public record Suggestion(Long bookId, String title, String author, String label) {

    public static Suggestion of(Long bookId, String title, String author) {
        return new Suggestion(bookId, title, author, author == null ? title : title + " — " + author);
    }
}
