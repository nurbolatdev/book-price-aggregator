package kz.nurbolat.bookaggregator.admin.dto;

/**
 * @param query    normalized query
 * @param searches how many times it was searched in the period
 */
public record SearchQueryStat(String query, long searches) {
}
