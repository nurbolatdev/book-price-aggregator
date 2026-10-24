package kz.nurbolat.bookaggregator.admin.dto;

/**
 * @param requeued messages returned to their original exchange
 * @param skipped  messages without a usable {@code x-death} header, kept in the dead-letter queue
 */
public record RequeueResponse(int requeued, int skipped) {
}
