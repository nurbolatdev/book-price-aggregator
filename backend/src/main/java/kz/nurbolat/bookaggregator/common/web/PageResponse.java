package kz.nurbolat.bookaggregator.common.web;

import java.util.List;

/**
 * Page of results in the format of docs/03-api-contract.md.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(List.copyOf(content), page, size, totalElements, totalPages);
    }
}
