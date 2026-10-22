package kz.nurbolat.bookaggregator.parser.core;

/**
 * The page does not exist (404 / 410), e.g. a product removed from the store. Not retried.
 */
public class PageNotFoundException extends FetchException {

    public PageNotFoundException(String url, int status) {
        super("Page not found (HTTP %d): %s".formatted(status, url));
    }
}
