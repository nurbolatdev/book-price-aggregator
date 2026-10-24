package kz.nurbolat.bookaggregator.admin;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Writes an admin journal entry when a store's circuit breaker opens (WARN) or closes again (INFO).
 * Breakers are created on first use, so the listener is attached to every breaker as it is added.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CircuitBreakerEventLogger {

    private final CircuitBreakerRegistry registry;
    private final SystemEventService events;

    @PostConstruct
    void register() {
        registry.getAllCircuitBreakers().forEach(this::listen);
        registry.getEventPublisher().onEntryAdded(added -> listen(added.getAddedEntry()));
    }

    private void listen(CircuitBreaker breaker) {
        breaker.getEventPublisher().onStateTransition(this::onTransition);
    }

    private void onTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.State to = event.getStateTransition().getToState();
        Map<String, Object> details = Map.of("breaker", event.getCircuitBreakerName(),
                "from", event.getStateTransition().getFromState().name(), "to", to.name());
        try {
            if (to == CircuitBreaker.State.OPEN || to == CircuitBreaker.State.FORCED_OPEN) {
                events.warn("circuit-breaker", "Circuit breaker " + event.getCircuitBreakerName() + " открыт", details);
            } else if (to == CircuitBreaker.State.CLOSED) {
                events.info("circuit-breaker", "Circuit breaker " + event.getCircuitBreakerName() + " закрыт", details);
            }
        } catch (RuntimeException e) {
            log.warn("Could not write the circuit breaker event: {}", e.getMessage());
        }
    }
}
