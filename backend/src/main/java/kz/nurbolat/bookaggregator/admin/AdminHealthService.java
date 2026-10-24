package kz.nurbolat.bookaggregator.admin;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import kz.nurbolat.bookaggregator.admin.dto.AdminHealthResponse;
import kz.nurbolat.bookaggregator.admin.dto.AdminHealthResponse.ComponentHealth;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Health of the services from Spring Boot's {@link HealthEndpoint} plus a few metrics. A failing
 * check or metric never fails the request: it shows as DOWN or an empty value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminHealthService {

    private static final double P95 = 0.95;

    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;

    public AdminHealthResponse health() {
        HealthComponent root = healthEndpoint.health();
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        if (root instanceof CompositeHealth composite) {
            composite.getComponents().forEach((name, descriptor) -> components.put(name, component(descriptor)));
        }
        return new AdminHealthResponse(root.getStatus().getCode(), components, metrics());
    }

    private static ComponentHealth component(HealthComponent descriptor) {
        String details = null;
        if (descriptor instanceof Health indicated && indicated.getDetails().get("error") != null) {
            details = String.valueOf(indicated.getDetails().get("error"));
        }
        return new ComponentHealth(descriptor.getStatus().getCode(), details);
    }

    private AdminHealthResponse.Metrics metrics() {
        return new AdminHealthResponse.Metrics(httpP95Millis(), (int) gauge("hikaricp.connections.active"),
                (int) gauge("hikaricp.connections.max"), redisUsedMemory());
    }

    /** Needs percentiles enabled for http.server.requests (application.yml). */
    private Double httpP95Millis() {
        return meterRegistry.find("http.server.requests").timers().stream()
                .filter(timer -> {
                    String uri = timer.getId().getTag("uri");
                    return uri == null || !uri.startsWith("/actuator");
                })
                .map(Timer::takeSnapshot)
                .flatMap(snapshot -> java.util.Arrays.stream(snapshot.percentileValues()))
                .filter(value -> value.percentile() == P95)
                .mapToDouble(value -> value.value(TimeUnit.MILLISECONDS))
                .max().stream().boxed().findFirst().orElse(null);
    }

    private double gauge(String name) {
        Gauge gauge = meterRegistry.find(name).gauge();
        return gauge == null ? 0 : gauge.value();
    }

    private Long redisUsedMemory() {
        try {
            return redisTemplate.execute(connection -> {
                Object used = connection.serverCommands().info("memory").get("used_memory");
                return used == null ? null : Long.parseLong(used.toString());
            }, true);
        } catch (RuntimeException e) {
            log.debug("Redis memory is unavailable: {}", e.getMessage());
            return null;
        }
    }
}
