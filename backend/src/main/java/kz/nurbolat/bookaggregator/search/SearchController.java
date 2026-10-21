package kz.nurbolat.bookaggregator.search;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.search.dto.SearchItem;
import kz.nurbolat.bookaggregator.search.dto.SearchRequest;
import kz.nurbolat.bookaggregator.search.dto.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final SuggestionService suggestionService;

    @GetMapping
    ResponseEntity<PageResponse<SearchItem>> search(@Valid @ParameterObject SearchRequest request) {
        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/suggest")
    ResponseEntity<List<Suggestion>> suggest(@RequestParam @NotBlank @Size(min = 2, max = 200) String q) {
        return ResponseEntity.ok(suggestionService.suggest(q));
    }
}
