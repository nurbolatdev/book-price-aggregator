package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.beacon.DigestService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every morning at 09:00 Almaty time the users who chose {@code DAILY} get one digest of their beacons.
 */
@Component
@RequiredArgsConstructor
public class DigestScheduler {

    private final DigestService digestService;

    @Scheduled(cron = "${app.scheduler.digest-cron:0 0 9 * * *}", zone = SchedulerProperties.ZONE)
    @SchedulerLock(name = "beacon-daily-digest", lockAtMostFor = "PT30M")
    public void sendDailyDigests() {
        digestService.sendDailyDigests();
    }
}
