package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.edition.dto.SimilarEdition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/** "Читайте также" block of the edition page; the result is cached for an hour. */
@Slf4j
@Service
public class SimilarEditionsService {

    private final EditionRepository editionRepository;
    private final SimilarEditionsRepository repository;
    private final Cache cache;

    public SimilarEditionsService(EditionRepository editionRepository, SimilarEditionsRepository repository,
                                  CacheManager cacheManager) {
        this.editionRepository = editionRepository;
        this.repository = repository;
        this.cache = Objects.requireNonNull(cacheManager.getCache(RedisConfig.SIMILAR));
    }

    @SuppressWarnings("unchecked")
    public List<SimilarEdition> getSimilar(long editionId, int limit) {
        if (!editionRepository.existsById(editionId)) {
            throw new NotFoundException("Издание не найдено");
        }
        String key = editionId + ":" + limit;
        try {
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null) {
                return (List<SimilarEdition>) cached.get();
            }
        } catch (DataAccessException e) {
            log.warn("Similar editions cache unavailable: {}", e.getMessage());
        }
        List<SimilarEdition> result = repository.findSimilar(editionId, limit);
        try {
            cache.put(key, result);
        } catch (DataAccessException e) {
            log.warn("Could not store similar editions in Redis: {}", e.getMessage());
        }
        return result;
    }
}
