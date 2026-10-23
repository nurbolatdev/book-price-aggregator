package kz.nurbolat.bookaggregator.beacon;

import jakarta.validation.Valid;
import kz.nurbolat.bookaggregator.beacon.dto.BeaconResponse;
import kz.nurbolat.bookaggregator.beacon.dto.CreateBeaconRequest;
import kz.nurbolat.bookaggregator.beacon.dto.UpdateBeaconRequest;
import kz.nurbolat.bookaggregator.scheduler.RefreshOnDemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beacons")
@RequiredArgsConstructor
public class BeaconController {

    private final BeaconService beaconService;
    private final RefreshOnDemandService refreshOnDemandService;

    @GetMapping
    List<BeaconResponse> list(@RequestParam(required = false) BeaconStatus status) {
        return beaconService.list(status);
    }

    /** The edition's prices are refreshed in the background if they are stale, so the beacon starts from fresh data. */
    @PostMapping
    ResponseEntity<BeaconResponse> create(@Valid @RequestBody CreateBeaconRequest request) {
        BeaconResponse beacon = beaconService.create(request);
        refreshOnDemandService.requestIfStale(List.of(beacon.editionId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(beacon);
    }

    @PatchMapping("/{id}")
    BeaconResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBeaconRequest request) {
        return beaconService.update(id, request);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) {
        beaconService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
