package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.beacon.BeaconEmailRelay;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every minute: beacon emails that never reached RabbitMQ are published again.
 */
@Component
@RequiredArgsConstructor
public class BeaconEmailRelayScheduler {

    private final BeaconEmailRelay relay;

    @Scheduled(fixedDelayString = "${app.scheduler.email-relay-delay:PT1M}")
    @SchedulerLock(name = "beacon-email-relay", lockAtMostFor = "PT5M")
    public void republishStuckEmails() {
        relay.run();
    }
}
