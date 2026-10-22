package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.store.StoreCode;

/**
 * Requests to the store are paused: its circuit breaker is open or no request slot was free in time.
 */
public class StoreUnavailableException extends FetchException {

    public StoreUnavailableException(StoreCode store, Throwable cause) {
        super("Store %s is temporarily unavailable: %s".formatted(store, cause.getMessage()), cause);
    }
}
