package kz.nurbolat.bookaggregator.search.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.search.SearchSort;
import kz.nurbolat.bookaggregator.store.StoreCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * Query parameters of GET /api/v1/search; missing values get the contract defaults.
 */
public record SearchRequest(
        @Schema(description = "Название, автор или ISBN; необязателен: без него — каталог", example = "мастер маргарита")
        @Size(min = 2, max = 200, message = "От 2 до 200 символов") String q,
        @Schema(description = "Коды магазинов через запятую", example = "FLIP,KASPI")
        List<StoreCode> stores,
        @Schema(description = "Минимальная цена в тенге", example = "1000") @PositiveOrZero(message = "Не меньше 0") BigDecimal minPrice,
        @Schema(description = "Максимальная цена в тенге", example = "10000") @PositiveOrZero(message = "Не меньше 0") BigDecimal maxPrice,
        @Schema(description = "Только издания в наличии", defaultValue = "true") Boolean inStock,
        @Schema(description = "Переплёты через запятую", example = "HARDCOVER,PAPERBACK") List<Binding> binding,
        @Schema(description = "Языки через запятую: ru, kk, en", example = "ru,kk")
        List<@Pattern(regexp = "ru|kk|en", message = "Допустимо ru, kk или en") String> language,
        @Schema(description = "Сортировка; по умолчанию RELEVANCE с запросом q и STORES_DESC без него")
        SearchSort sort,
        @Schema(description = "Номер страницы с нуля", defaultValue = "0") @Min(value = 0, message = "Не меньше 0") Integer page,
        @Schema(description = "Размер страницы, до 50", defaultValue = "20") @Min(value = 1, message = "Не меньше 1") @Max(value = 50, message = "Не больше 50") Integer size
) {

    public SearchRequest {
        stores = stores == null ? List.of() : List.copyOf(stores);
        inStock = inStock == null ? Boolean.TRUE : inStock;
        q = q == null || q.isBlank() ? null : q;
        binding = binding == null ? List.of() : List.copyOf(binding);
        language = language == null ? List.of() : List.copyOf(language);
        sort = sort == null ? (q == null ? SearchSort.STORES_DESC : SearchSort.RELEVANCE) : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }

    @Schema(hidden = true)
    @JsonIgnore
    @AssertTrue(message = "Сортировка RELEVANCE доступна только с запросом q")
    boolean isSort() {
        return q != null || sort != SearchSort.RELEVANCE;
    }
}
