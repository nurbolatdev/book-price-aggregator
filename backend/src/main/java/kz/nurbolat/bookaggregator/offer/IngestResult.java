package kz.nurbolat.bookaggregator.offer;

/**
 * Outcome of storing a batch of parsed offers.
 *
 * @param upserted      offers created or updated
 * @param priceChanged  new offers plus offers whose price changed (each got a price_history row)
 * @param matched       offers newly linked to an edition
 * @param pendingReview offers left for manual matching
 */
public record IngestResult(int upserted, int priceChanged, int matched, int pendingReview) {

    public static final IngestResult EMPTY = new IngestResult(0, 0, 0, 0);

    public IngestResult plus(IngestResult other) {
        return new IngestResult(upserted + other.upserted, priceChanged + other.priceChanged,
                matched + other.matched, pendingReview + other.pendingReview);
    }
}
