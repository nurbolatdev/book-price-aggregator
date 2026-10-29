package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.home.WeeklyDigestService;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every Monday at 10:00 Almaty time the users with the weekly digest switched on get the week's biggest drops.
 */
@Component
@RequiredArgsConstructor
public class WeeklyDigestScheduler {

    private final WeeklyDigestService weeklyDigestService;

    @Scheduled(cron = "${app.scheduler.weekly-digest-cron:0 0 10 * * MON}", zone = SchedulerProperties.ZONE)
    @SchedulerLock(name = "weekly-digest", lockAtMostFor = "PT30M")
    public void sendWeeklyDigests() {
        weeklyDigestService.sendWeeklyDigests();
    }
}
