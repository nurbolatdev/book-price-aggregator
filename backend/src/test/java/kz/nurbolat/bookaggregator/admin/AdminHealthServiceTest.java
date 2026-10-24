package kz.nurbolat.bookaggregator.admin;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kz.nurbolat.bookaggregator.admin.dto.AdminHealthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminHealthServiceTest {

    private final HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final AdminHealthService service = new AdminHealthService(healthEndpoint, new SimpleMeterRegistry(), redis);

    @Test
    void shouldReportDownComponentAndItsError() {
        Map<String, org.springframework.boot.actuate.health.HealthComponent> parts = new LinkedHashMap<>();
        parts.put("db", Health.up().build());
        parts.put("redis", Health.down().withDetail("error", "Connection refused").build());
        CompositeHealth root = mock(CompositeHealth.class);
        when(root.getStatus()).thenReturn(Status.DOWN);
        when(root.getComponents()).thenReturn(parts);
        when(healthEndpoint.health()).thenReturn(root);
        when(redis.execute(any(RedisCallback.class), anyBoolean())).thenThrow(new IllegalStateException("redis is down"));

        AdminHealthResponse response = service.health();

        assertThat(response.status()).isEqualTo("DOWN");
        assertThat(response.components().get("db").status()).isEqualTo("UP");
        assertThat(response.components().get("redis").status()).isEqualTo("DOWN");
        assertThat(response.components().get("redis").details()).isEqualTo("Connection refused");
        assertThat(response.metrics().redisUsedMemoryBytes()).isNull();
    }
}
