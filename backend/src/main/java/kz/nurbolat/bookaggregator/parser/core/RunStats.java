package kz.nurbolat.bookaggregator.parser.core;

/**
 * Totals of a finished run.
 *
 * @param aborted {@code true} when the run was stopped early (store blocked or unavailable)
 * @param error   reason of the stop or of the last page failure, {@code null} if none
 */
public record RunStats(int pagesOk, int pagesFailed, int offersUpserted, int pricesChanged,
                       boolean aborted, String error) {
}
