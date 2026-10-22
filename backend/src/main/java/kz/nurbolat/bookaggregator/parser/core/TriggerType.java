package kz.nurbolat.bookaggregator.parser.core;

/**
 * What started a parser run.
 */
public enum TriggerType {
    /** Regular schedule. */
    SCHEDULED,
    /** Stale offers seen by a user request. */
    ON_DEMAND,
    /** Started by an administrator. */
    MANUAL
}
