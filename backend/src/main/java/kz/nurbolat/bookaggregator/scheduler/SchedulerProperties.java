package kz.nurbolat.bookaggregator.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Scheduled jobs ({@code app.scheduler.*}); cron expressions are in Asia/Almaty time.
 *
 * @param crawlCron       regular crawl of every switched-on store
 * @param cleanupCron     daily cleanup: stale offers, old search log, expired tokens
 * @param digestCron      daily digest of beacon notifications for users who chose {@code DAILY}
 * @param weeklyDigestCron weekly email with the biggest price drops for users who left {@code weeklyDigest} on
 * @param staleOfferAfter an offer not seen in its store for this long is out of stock
 * @param searchLogKeep   search log entries older than this are deleted
 * @param freshFor        prices of an edition seen in a store within this time need no refresh on demand
 * @param pendingFor      an on-demand refresh of an edition in a store is not queued again within this time
 */
@ConfigurationProperties("app.scheduler")
public record SchedulerProperties(
        @DefaultValue("0 0 */6 * * *") String crawlCron,
        @DefaultValue("0 30 3 * * *") String cleanupCron,
        @DefaultValue("0 0 9 * * *") String digestCron,
        @DefaultValue("0 0 10 * * MON") String weeklyDigestCron,
        @DefaultValue("7d") Duration staleOfferAfter,
        @DefaultValue("90d") Duration searchLogKeep,
        @DefaultValue("6h") Duration freshFor,
        @DefaultValue("30m") Duration pendingFor
) {

    public static final String ZONE = "Asia/Almaty";
}
