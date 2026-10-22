package kz.nurbolat.bookaggregator.parser.core;

/**
 * A store page could not be loaded. Subclasses tell the fetcher whether to retry.
 */
public class FetchException extends RuntimeException {

    public FetchException(String message) {
        super(message);
    }

    public FetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
