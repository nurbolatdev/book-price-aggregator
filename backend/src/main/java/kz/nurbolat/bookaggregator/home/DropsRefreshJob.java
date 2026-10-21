package kz.nurbolat.bookaggregator.home;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recalculates the weekly drops every 15 minutes (cluster-wide locking arrives with ShedLock in M17).
 */
@Component
@RequiredArgsConstructor
class DropsRefreshJob {

    private final DropsService dropsService;

    @Scheduled(fixedRateString = "PT15M", initialDelayString = "PT10S")
    void refresh() {
        dropsService.refresh();
    }
}
