package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.offer.IngestResult;
import kz.nurbolat.bookaggregator.store.StoreCode;

/**
 * Outcome of one parser run.
 */
public record CrawlResult(
        Long runId,
        StoreCode store,
        RunStatus status,
        int pagesOk,
        int pagesFailed,
        IngestResult ingest,
        String error
) {
}
