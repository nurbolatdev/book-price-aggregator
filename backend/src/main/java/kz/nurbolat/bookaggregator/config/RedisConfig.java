package kz.nurbolat.bookaggregator.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.Suggestion;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * Named Redis caches (docs/01-architecture.md, section 5). Every cache has its own key prefix
 * with a format version, its own TTL and a serializer bound to the cached type.
 */
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    public static final String SEARCH = "search";
    public static final String SUGGEST = "suggest";
    public static final String EDITION = "edition";

    /** Defaults for caches without their own configuration. */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()));
    }

    @Bean
    RedisCacheManagerBuilderCustomizer namedCaches(RedisCacheConfiguration defaults, ObjectMapper objectMapper) {
        TypeFactory types = objectMapper.getTypeFactory();
        return builder -> builder
                .withCacheConfiguration(SEARCH, cache(defaults, objectMapper, "search:v1:", Duration.ofMinutes(10),
                        types.constructParametricType(PageResponse.class, SearchItem.class)))
                .withCacheConfiguration(SUGGEST, cache(defaults, objectMapper, "suggest:v1:", Duration.ofMinutes(10),
                        types.constructCollectionType(List.class, Suggestion.class)))
                .withCacheConfiguration(EDITION, cache(defaults, objectMapper, "edition:v1:", Duration.ofMinutes(30),
                        types.constructType(EditionDetailResponse.class)));
    }

    /** A Redis outage degrades to uncached reads instead of failing requests. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private static RedisCacheConfiguration cache(RedisCacheConfiguration defaults, ObjectMapper objectMapper,
                                                 String prefix, Duration ttl, JavaType type) {
        return defaults
                .entryTtl(ttl)
                .computePrefixWith(cacheName -> prefix)
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new Jackson2JsonRedisSerializer<>(objectMapper, type)));
    }
}
