package kz.nurbolat.bookaggregator.admin;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/admin/offers")
@RequiredArgsConstructor
public class AdminOfferController {

    private final OfferReviewService offerReviewService;

    @Operation(summary = "Offers the matcher left for manual review")
    @GetMapping("/pending-review")
    ResponseEntity<PageResponse<ReviewOfferResponse>> pendingReview(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(offerReviewService.pendingReview(page, size));
    }

    @Operation(summary = "Link an offer to an edition")
    @PostMapping("/{offerId}/match")
    ResponseEntity<ReviewOfferResponse> match(@PathVariable Long offerId,
                                              @Valid @RequestBody OfferMatchRequest request) {
        return ResponseEntity.ok(offerReviewService.match(offerId, request.editionId()));
    }

    @Operation(summary = "Reject an offer that is not a book")
    @PostMapping("/{offerId}/reject")
    ResponseEntity<ReviewOfferResponse> reject(@PathVariable Long offerId) {
        return ResponseEntity.ok(offerReviewService.reject(offerId));
    }
}
