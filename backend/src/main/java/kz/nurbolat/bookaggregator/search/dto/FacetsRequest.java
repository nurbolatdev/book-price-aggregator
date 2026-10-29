package kz.nurbolat.bookaggregator.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Query parameters of GET /api/v1/search/facets: the filters of the search request without sort and paging.
 */
public record FacetsRequest(
        @Schema(description = "Название, автор или ISBN; необязателен: без него — каталог", example = "мастер маргарита")
        @Size(min = 2, max = 200, message = "От 2 до 200 символов") String q,
        @Schema(description = "Коды магазинов через запятую", example = "FLIP,KASPI")
        List<StoreCode> stores,
        @Schema(description = "Минимальная цена в тенге", example = "1000") @PositiveOrZero(message = "Не меньше 0") BigDecimal minPrice,
        @Schema(description = "Максимальная цена в тенге", example = "10000") @PositiveOrZero(message = "Не меньше 0") BigDecimal maxPrice,
        @Schema(description = "Только издания в наличии", defaultValue = "true") Boolean inStock,
        @Schema(description = "Переплёты через запятую", example = "HARDCOVER,PAPERBACK") List<Binding> binding,
        @Schema(description = "Языки через запятую: ru, kk, en", example = "ru,kk")
        List<@Pattern(regexp = "ru|kk|en", message = "Допустимо ru, kk или en") String> language
) {

    public FacetsRequest {
        q = q == null || q.isBlank() ? null : q;
    }

    public SearchRequest toSearchRequest() {
        return new SearchRequest(q, stores, minPrice, maxPrice, inStock, binding, language, null, 0, 1);
    }
}
