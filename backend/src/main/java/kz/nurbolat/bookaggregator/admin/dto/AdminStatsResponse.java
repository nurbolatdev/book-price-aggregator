package kz.nurbolat.bookaggregator.admin.dto;

/**
 * KPI of the admin overview for one period.
 *
 * @param cacheHitPercent share of cached search pages from Micrometer counters since the app start, null before the first search
 */
public record AdminStatsResponse(
        String period,
        Users users,
        long activeUsers,
        Beacons beacons,
        Emails emails,
        long searches,
        Double cacheHitPercent,
        Catalog catalog
) {

    public record Users(long total, long newInPeriod, long verified) {
    }

    public record Beacons(long active, long triggeredInPeriod) {
    }

    public record Emails(long sent, long failed) {
    }

    public record Catalog(long books, long editions, long matchedOffers, long pendingReview) {
    }
}
