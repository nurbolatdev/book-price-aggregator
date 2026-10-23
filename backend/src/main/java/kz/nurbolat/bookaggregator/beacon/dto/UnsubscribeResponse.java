package kz.nurbolat.bookaggregator.beacon.dto;

import kz.nurbolat.bookaggregator.beacon.BeaconStatus;

/**
 * The beacon that was paused, so that the page can say which book it was.
 */
public record UnsubscribeResponse(Long beaconId, Long editionId, String title, BeaconStatus status) {
}
