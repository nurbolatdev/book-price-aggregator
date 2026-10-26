package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kz.nurbolat.bookaggregator.admin.dto.OfferMatchRequest;
import kz.nurbolat.bookaggregator.common.web.PageResponse;
import kz.nurbolat.bookaggregator.offer.OfferReviewService;
import kz.nurbolat.bookaggregator.offer.dto.ReviewOfferResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: Offers", description = "Ручная проверка предложений")
@RequestMapping("/api/v1/admin/offers")
@RequiredArgsConstructor
public class AdminOfferController {

    private final OfferReviewService offerReviewService;

    @Operation(summary = "Предложения на проверку", description = "Предложения, которые матчер не смог привязать к изданию.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @GetMapping("/pending-review")
    ResponseEntity<PageResponse<ReviewOfferResponse>> pendingReview(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(offerReviewService.pendingReview(page, size));
    }

    @Operation(summary = "Привязать предложение", description = "Привязывает предложение к изданию.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "404", description = "Не найдено")
    @ApiResponse(responseCode = "422", description = "Ошибка валидации")
    @PostMapping("/{offerId}/match")
    ResponseEntity<ReviewOfferResponse> match(@PathVariable Long offerId,
                                              @Valid @RequestBody OfferMatchRequest request) {
        return ResponseEntity.ok(offerReviewService.match(offerId, request.editionId()));
    }

    @Operation(summary = "Отклонить предложение", description = "Помечает предложение как не относящееся к книгам.")
    @ApiResponse(responseCode = "403", description = "Нужна роль ADMIN")
    @ApiResponse(responseCode = "404", description = "Не найдено")
    @PostMapping("/{offerId}/reject")
    ResponseEntity<ReviewOfferResponse> reject(@PathVariable Long offerId) {
        return ResponseEntity.ok(offerReviewService.reject(offerId));
    }
}
