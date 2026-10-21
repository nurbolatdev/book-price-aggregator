package kz.nurbolat.bookaggregator.search.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
        @Schema(description = "Название, автор или ISBN", example = "мастер маргарита")
        @NotBlank @Size(min = 2, max = 200) String q,
        @Schema(description = "Коды магазинов через запятую", example = "FLIP,KASPI")
        List<StoreCode> stores,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice,
        @Schema(defaultValue = "true") Boolean inStock,
        Binding binding,
        @Pattern(regexp = "ru|kk|en") String language,
        @Schema(defaultValue = "RELEVANCE") SearchSort sort,
        @Schema(defaultValue = "0") @Min(0) Integer page,
        @Schema(defaultValue = "20") @Min(1) @Max(50) Integer size
) {

    public SearchRequest {
        stores = stores == null ? List.of() : List.copyOf(stores);
        inStock = inStock == null ? Boolean.TRUE : inStock;
        sort = sort == null ? SearchSort.RELEVANCE : sort;
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
    }
}
