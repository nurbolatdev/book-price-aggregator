package kz.nurbolat.bookaggregator.admin.dto;

/**
 * @param queued the crawl task is in the store's queue
 */
public record ParserRunQueuedResponse(boolean queued) {
}
