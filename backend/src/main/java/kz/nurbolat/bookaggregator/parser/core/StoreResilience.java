package kz.nurbolat.bookaggregator.parser.core;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.store.StoreService;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Per-store limits shared by every way of loading pages (plain HTTP and the browser): one rate
 * limiter ({@code stores.min_delay_ms}), one circuit breaker and the retry policy per store, looked
 * up by name in the registries, so a store is paused for all fetchers at once.
 */
class StoreResilience {

    private final ParserProperties properties;
    private final StoreService storeService;
    private final CircuitBreakerRegistry circuitBreakers;
    private final RateLimiterRegistry rateLimiters;
    private final RetryRegistry retries;

    StoreResilience(ParserProperties properties, StoreService storeService, CircuitBreakerRegistry circuitBreakers,
                    RateLimiterRegistry rateLimiters, RetryRegistry retries) {
        this.properties = properties;
        this.storeService = storeService;
        this.circuitBreakers = circuitBreakers;
        this.rateLimiters = rateLimiters;
        this.retries = retries;
    }

    /**
     * Runs one request to the store: waits for its rate limit, goes through its circuit breaker and
     * retries {@link RetryableFetchException}s. A {@link StoreBlockedException} opens the breaker at once.
     *
     * @throws StoreUnavailableException the store is paused or no request slot was free in time
     */
    <T> T call(StoreCode store, Supplier<T> request) {
        CircuitBreaker circuitBreaker = circuitBreaker(store);
        Supplier<T> guarded = Retry.decorateSupplier(retry(store),
                CircuitBreaker.decorateSupplier(circuitBreaker,
                        RateLimiter.decorateSupplier(rateLimiter(store), request)));
        try {
            return guarded.get();
        } catch (StoreBlockedException e) {
            // Stop all further requests to this store for the open period
            circuitBreaker.transitionToOpenState();
            throw e;
        } catch (CallNotPermittedException | RequestNotPermitted e) {
            throw new StoreUnavailableException(store, e);
        }
    }

    private CircuitBreaker circuitBreaker(StoreCode store) {
        return circuitBreakers.circuitBreaker("store-" + store, () -> CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(properties.circuitBreakerOpenDuration())
                .recordExceptions(RetryableFetchException.class, StoreBlockedException.class)
                .ignoreExceptions(PageNotFoundException.class)
                .build());
    }

    private RateLimiter rateLimiter(StoreCode store) {
        Duration minDelay = Duration.ofMillis(Math.max(1, storeService.getByCode(store).getMinDelayMs()));
        return rateLimiters.rateLimiter("store-" + store, () -> RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(minDelay)
                .timeoutDuration(properties.rateLimitMaxWait())
                .build());
    }

    private Retry retry(StoreCode store) {
        return retries.retry("store-" + store, () -> RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(properties.retryWait())
                .retryExceptions(RetryableFetchException.class)
                .build());
    }
}
