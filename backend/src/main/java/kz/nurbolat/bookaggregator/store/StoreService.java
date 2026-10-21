package kz.nurbolat.bookaggregator.store;

import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Store reference data. There are only a handful of stores and they rarely change,
 * so the list is kept in memory; {@link #evictCache()} is called after an admin update.
 */
@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final AtomicReference<List<Store>> cache = new AtomicReference<>();

    public List<Store> findAll() {
        List<Store> stores = cache.get();
        if (stores == null) {
            stores = List.copyOf(storeRepository.findAllByOrderByIdAsc());
            cache.set(stores);
        }
        return stores;
    }

    public Store getByCode(StoreCode code) {
        return findAll().stream()
                .filter(store -> store.getCode() == code)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Store not found: " + code));
    }

    public void evictCache() {
        cache.set(null);
    }
}
