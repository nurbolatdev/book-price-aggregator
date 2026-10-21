package kz.nurbolat.bookaggregator.store.dto;

import kz.nurbolat.bookaggregator.store.Store;
import kz.nurbolat.bookaggregator.store.StoreCode;

public record StoreResponse(StoreCode code, String name, boolean enabled) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getCode(), store.getName(), store.isEnabled());
    }
}
