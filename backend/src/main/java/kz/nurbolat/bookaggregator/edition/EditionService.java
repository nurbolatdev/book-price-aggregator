package kz.nurbolat.bookaggregator.edition;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.config.RedisConfig;
import kz.nurbolat.bookaggregator.edition.EditionPriceRepository.EditionPrice;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse.BestOffer;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse.BookInfo;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse.OfferInfo;
import kz.nurbolat.bookaggregator.edition.dto.EditionDetailResponse.OtherEdition;
import kz.nurbolat.bookaggregator.edition.dto.PriceVerdict;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.offer.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EditionService {

    private final EditionRepository editionRepository;
    private final EditionPriceRepository editionPriceRepository;
    private final OfferService offerService;
    private final PriceVerdictCalculator priceVerdictCalculator;
    private final Clock clock;

    /**
     * Edition card in four queries: edition with book, offers with stores, editions of the book
     * with best prices, daily minimums of the last 90 days for the verdict. Cached for 30 minutes.
     */
    @Cacheable(cacheNames = RedisConfig.EDITION, key = "#id")
    @Transactional(readOnly = true)
    public EditionDetailResponse getDetail(Long id) {
        Edition edition = editionRepository.findWithBookById(id)
                .orElseThrow(() -> new NotFoundException("Edition not found: " + id));
        Book book = edition.getBook();

        List<OfferInfo> offers = offerService.findMatchedOffers(id).stream()
                .map(EditionService::toOfferInfo)
                .toList();
        BestOffer bestPrice = offers.stream()
                .filter(OfferInfo::inStock)
                .findFirst()
                .map(offer -> new BestOffer(offer.store(), offer.price(), offer.deliveryText(), offer.url()))
                .orElse(null);

        List<OtherEdition> otherEditions = editionPriceRepository.findEditionsOfBook(book.getId()).stream()
                .filter(other -> !other.editionId().equals(id))
                .map(other -> new OtherEdition(other.editionId(), label(other), other.bestPrice()))
                .toList();

        LocalDate today = LocalDate.now(clock);
        List<DailyPrice> dailyMinimums = editionPriceRepository.findDailyMinimums(
                id, today.minusDays(PriceVerdictCalculator.PERIOD_DAYS), today);
        PriceVerdict verdict = priceVerdictCalculator.calculate(
                dailyMinimums, bestPrice == null ? null : bestPrice.price(), today);

        return new EditionDetailResponse(
                edition.getId(),
                new BookInfo(book.getId(), book.getTitle(), book.getAuthor(), book.getDescription()),
                edition.getIsbn13(),
                edition.getPublisher(),
                edition.getYear() == null ? null : edition.getYear().intValue(),
                edition.getBinding(),
                edition.getLanguage(),
                edition.getPages(),
                edition.getCoverUrl(),
                otherEditions,
                bestPrice,
                verdict,
                offers
        );
    }

    /**
     * Lowest in-stock price of every edition of a book; editions without offers are absent.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> findBestPricesOfBook(Long bookId) {
        return editionPriceRepository.findEditionsOfBook(bookId).stream()
                .filter(edition -> edition.bestPrice() != null)
                .collect(Collectors.toMap(EditionPrice::editionId, EditionPrice::bestPrice));
    }

    private static OfferInfo toOfferInfo(Offer offer) {
        return new OfferInfo(
                offer.getId(),
                offer.getStore().getCode(),
                offer.getStore().getName(),
                offer.getPrice(),
                offer.getOldPrice(),
                offer.isInStock(),
                offer.getDeliveryText(),
                offer.getUrl(),
                offer.getLastSeenAt()
        );
    }

    // "Азбука 2022 · мягкий"
    static String label(EditionPrice edition) {
        String publisherAndYear = String.join(" ", Stream.of(
                        edition.publisher(), edition.year() == null ? null : edition.year().toString())
                .filter(part -> part != null && !part.isBlank())
                .toList());
        String binding = switch (edition.binding()) {
            case HARDCOVER -> "твёрдый";
            case PAPERBACK -> "мягкий";
            case UNKNOWN -> null;
        };
        if (publisherAndYear.isEmpty()) {
            return binding == null ? "Издание №" + edition.editionId() : binding;
        }
        return binding == null ? publisherAndYear : publisherAndYear + " · " + binding;
    }
}
