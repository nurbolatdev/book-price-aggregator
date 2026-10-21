package kz.nurbolat.bookaggregator.search;

import kz.nurbolat.bookaggregator.common.text.TextNormalizer;
import kz.nurbolat.bookaggregator.search.dto.Suggestion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuggestionService {

    static final int LIMIT = 8;

    private final SearchRepository searchRepository;

    /**
     * Up to eight "title — author" variants for the typed text.
     */
    public List<Suggestion> suggest(String query) {
        String normalized = TextNormalizer.normalize(query);
        if (normalized == null || normalized.length() < 2) {
            return List.of();
        }
        return searchRepository.findSuggestions(normalized, LIMIT);
    }
}
