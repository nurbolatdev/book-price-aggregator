package kz.nurbolat.bookaggregator.search;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    ResponseEntity<PageResponse<SearchItem>> search(@Valid @ParameterObject SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }
}
