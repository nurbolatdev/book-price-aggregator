package kz.nurbolat.bookaggregator.beacon;

import kz.nurbolat.bookaggregator.auth.EmailTokenService;
import kz.nurbolat.bookaggregator.beacon.dto.BeaconResponse;
import kz.nurbolat.bookaggregator.beacon.dto.CreateBeaconRequest;
import kz.nurbolat.bookaggregator.beacon.dto.UnsubscribeResponse;
import kz.nurbolat.bookaggregator.beacon.dto.UpdateBeaconRequest;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.edition.EditionRepository;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.offer.OfferService;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * Beacons of the signed-in user. Someone else's beacon answers 404, as if it did not exist.
 */
@Service
@RequiredArgsConstructor
public class BeaconService {

    private final BeaconRepository beaconRepository;
    private final BeaconListRepository beaconListRepository;
    private final EditionRepository editionRepository;
    private final OfferService offerService;
    private final SecurityUtils securityUtils;
    private final EmailTokenService emailTokenService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<BeaconResponse> list(BeaconStatus status, Long editionId) {
        return beaconListRepository.findOfUser(currentUserId(), status, editionId);
    }

    /**
     * @throws AppException {@code CONFLICT} if the user already watches this edition
     */
    @Transactional
    public BeaconResponse create(CreateBeaconRequest request) {
        User user = securityUtils.getCurrentUser();
        Edition edition = editionRepository.findById(request.editionId())
                .orElseThrow(() -> new NotFoundException("Издание не найдено"));
        if (beaconRepository.existsByUserIdAndEditionId(user.getId(), edition.getId())) {
            throw duplicate();
        }
        Beacon beacon = new Beacon();
        beacon.setUser(user);
        beacon.setEdition(edition);
        setCondition(beacon, request.conditionType(), request.targetPrice(), request.targetPercent());
        arm(beacon);
        try {
            beaconRepository.saveAndFlush(beacon);
        } catch (DataIntegrityViolationException e) {
            // Two requests for the same edition at once: the unique constraint lets one through
            throw duplicate();
        }
        return response(user.getId(), beacon.getId());
    }

    /**
     * A new condition or a return to {@code ACTIVE} re-arms the beacon: the baseline becomes the current best
     * price and the next drop is reported again.
     */
    @Transactional
    public BeaconResponse update(Long id, UpdateBeaconRequest request) {
        Long userId = currentUserId();
        Beacon beacon = beaconRepository.findByIdAndUserId(id, userId).orElseThrow(BeaconService::notFound);
        boolean rearm = false;
        if (request.conditionType() != null) {
            setCondition(beacon, request.conditionType(), request.targetPrice(), request.targetPercent());
            rearm = beacon.getStatus() != BeaconStatus.PAUSED;
        }
        if (request.status() == BeaconStatus.PAUSED) {
            beacon.changeStatus(BeaconStatus.PAUSED, clock.instant());
            rearm = false;
        } else if (request.status() == BeaconStatus.ACTIVE && beacon.getStatus() != BeaconStatus.ACTIVE) {
            rearm = true;
        }
        if (rearm) {
            arm(beacon);
        }
        return response(userId, beacon.getId());
    }

    @Transactional
    public void delete(Long id) {
        Beacon beacon = beaconRepository.findByIdAndUserId(id, currentUserId()).orElseThrow(BeaconService::notFound);
        beaconRepository.delete(beacon);
    }

    /**
     * Pauses the beacon of an unsubscribe link from an email, without signing in. Clicking again is harmless.
     *
     * @throws AppException {@code TOKEN_INVALID} or {@code TOKEN_EXPIRED}
     */
    @Transactional
    public UnsubscribeResponse unsubscribe(String token) {
        Beacon beacon = emailTokenService.unsubscribeBeacon(token);
        beacon.changeStatus(BeaconStatus.PAUSED, clock.instant());
        return new UnsubscribeResponse(beacon.getId(), beacon.getEdition().getId(),
                beacon.getEdition().getBook().getTitle(), beacon.getStatus());
    }

    // Irrelevant parameters are cleared, so that a beacon never shows a target it does not use
    private static void setCondition(Beacon beacon, BeaconCondition type, BigDecimal targetPrice, Integer targetPercent) {
        beacon.setConditionType(type);
        beacon.setTargetPrice(type == BeaconCondition.BELOW_PRICE ? targetPrice : null);
        beacon.setTargetPercent(type == BeaconCondition.DISCOUNT_PERCENT ? targetPercent.shortValue() : null);
    }

    private void arm(Beacon beacon) {
        beacon.changeStatus(BeaconStatus.ACTIVE, clock.instant());
        beacon.setBaselinePrice(offerService.findBestOffer(beacon.getEdition().getId()).map(Offer::getPrice).orElse(null));
        beacon.setLastNotifiedPrice(null);
    }

    private BeaconResponse response(Long userId, Long beaconId) {
        beaconRepository.flush();
        return beaconListRepository.findOne(userId, beaconId).orElseThrow(BeaconService::notFound);
    }

    private Long currentUserId() {
        return securityUtils.currentUser().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED)).id();
    }

    private static AppException duplicate() {
        return new AppException(ErrorCode.CONFLICT, "На это издание уже есть маячок");
    }

    private static NotFoundException notFound() {
        return new NotFoundException("Маячок не найден");
    }
}
