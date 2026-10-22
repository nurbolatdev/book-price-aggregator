package kz.nurbolat.bookaggregator.parser.core;

/**
 * The store's robots.txt forbids the path; the request is not sent.
 */
public class DisallowedByRobotsException extends FetchException {

    public DisallowedByRobotsException(String url) {
        super("Disallowed by robots.txt: " + url);
    }
}
