package kz.nurbolat.bookaggregator.parser.core;

/**
 * Transient failure (5xx, timeout, connection error) that is worth a retry.
 */
public class RetryableFetchException extends FetchException {

    public RetryableFetchException(String message) {
        super(message);
    }

    public RetryableFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
