package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.store.StoreCode;

/**
 * The store refused us (403, 429 or a captcha). Never retried: the parser stops and the
 * store's circuit breaker opens, as required by the parsing policy.
 */
public class StoreBlockedException extends FetchException {

    public StoreBlockedException(StoreCode store, String url, String reason) {
        super("Store %s blocked request to %s: %s".formatted(store, url, reason));
    }
}
