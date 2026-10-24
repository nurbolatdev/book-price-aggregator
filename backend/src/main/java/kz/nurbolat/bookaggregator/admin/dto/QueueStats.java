package kz.nurbolat.bookaggregator.admin.dto;

/**
 * @param ackRate messages acknowledged per second by consumers, 0 if the queue was idle
 */
public record QueueStats(String name, long ready, long unacknowledged, int consumers, double ackRate) {
}
