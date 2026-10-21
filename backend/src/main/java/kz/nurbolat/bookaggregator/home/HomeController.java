package kz.nurbolat.bookaggregator.home;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.home.dto.DropItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final DropsService dropsService;

    @GetMapping("/drops")
    ResponseEntity<List<DropItem>> drops(@RequestParam(defaultValue = "8") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(dropsService.getDrops(limit));
    }
}
