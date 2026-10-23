package kz.nurbolat.bookaggregator.home;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recalculates the weekly drops every 15 minutes, on one instance at a time (ShedLock).
 */
@Component
@RequiredArgsConstructor
class DropsRefreshJob {

    private final DropsService dropsService;

    @Scheduled(fixedRateString = "PT15M", initialDelayString = "PT10S")
    @SchedulerLock(name = "drops-refresh", lockAtMostFor = "PT10M")
    void refresh() {
        dropsService.refresh();
    }
}
