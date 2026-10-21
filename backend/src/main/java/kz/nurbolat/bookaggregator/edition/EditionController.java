package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/editions")
@RequiredArgsConstructor
public class EditionController {

    private final EditionService editionService;

    @GetMapping("/{id}")
    ResponseEntity<EditionDetailResponse> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(editionService.getDetail(id));
    }
}
