package kz.nurbolat.bookaggregator.parser.core;

public enum RunStatus {
    RUNNING,
    /** Every page was processed. */
    SUCCESS,
    /** Some pages failed, the rest was processed. */
    PARTIAL,
    /** The run was stopped (store blocked or unavailable) or no page succeeded. */
    FAILED
}
